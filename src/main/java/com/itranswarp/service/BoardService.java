package com.itranswarp.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.itranswarp.bean.BoardBean;
import com.itranswarp.bean.ReplyBean;
import com.itranswarp.bean.TopicBean;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.RefType;
import com.itranswarp.markdown.Markdown;
import com.itranswarp.model.Board;
import com.itranswarp.model.Reply;
import com.itranswarp.model.Topic;
import com.itranswarp.model.User;
import com.itranswarp.warpdb.Page;
import com.itranswarp.warpdb.PagedResults;

@Component
public class BoardService extends AbstractService<Board> {

	@Autowired
	Markdown markdown;

	static final String KEY_BOARDS = "_boards";
	static final String KEY_TOPICS_FIRST_PAGE = "_topics_";
	static final long CACHE_TOPICS_SECONDS = 3600;

	String sqlUpdateBoardIncTopicNumber;
	String sqlUpdateBoardDecTopicNumber;
	String sqlUpdateTopicIncReplyNumber;
	String sqlUpdateTopicDecReplyNumber;
	String sqlDeleteReplies;

	@PostConstruct
	public void init() {
		String boardTable = this.db.getTable(Board.class);
		String topicTable = this.db.getTable(Topic.class);
		String replyTable = this.db.getTable(Reply.class);
		this.sqlUpdateBoardIncTopicNumber = "UPDATE " + boardTable + " SET topicNumber = topicNumber + 1 WHERE id = ?";
		this.sqlUpdateBoardDecTopicNumber = "UPDATE " + boardTable + " SET topicNumber = topicNumber - 1 WHERE id = ?";
		this.sqlUpdateTopicIncReplyNumber = "UPDATE " + topicTable + " SET replyNumber = replyNumber + 1 WHERE id = ?";
		this.sqlUpdateTopicDecReplyNumber = "UPDATE " + topicTable + " SET replyNumber = replyNumber - 1 WHERE id = ?";

		this.sqlDeleteReplies = "DELETE FROM " + replyTable + " WHERE topicId = ?";
	}

	public Board getBoardFromCache(Long id) {
		Board c = this.redisService.hget(KEY_BOARDS, id, Board.class);
		if (c == null) {
			c = getById(id);
			this.redisService.hset(KEY_BOARDS, id, c);
		}
		return c;
	}

	public void removeBoardsFromCache() {
		this.redisService.del(KEY_BOARDS);
	}

	public void removeBoardFromCache(String id) {
		this.redisService.hdel(KEY_BOARDS, id);
	}

	public List<Board> getBoards() {
		return this.db.from(Board.class).orderBy("displayOrder").list();
	}

	@Transactional
	public Board createBoard(BoardBean bean) {
		long maxDisplayOrder = getBoards().stream().mapToLong(c -> c.displayOrder).max().orElseGet(() -> 0);
		Board board = new Board();
		board.name = checkName(bean.name);
		board.description = checkDescription(bean.description);
		board.displayOrder = maxDisplayOrder + 1;
		board.tag = checkTag(bean.tag);
		this.db.insert(board);
		return board;
	}

	@Transactional
	public Board updateBoard(Long id, BoardBean bean) {
		Board board = this.getById(id);
		if (board.name != null) {
			board.name = checkName(board.name);
		}
		if (bean.tag != null) {
			board.tag = checkTag(board.tag);
		}
		if (bean.description != null) {
			board.description = checkDescription(board.description);
		}
		this.db.update(board);
		return board;
	}

	@Transactional
	public void deleteBoard(Long id) {
		Board board = this.getById(id);
		if (db.from(Topic.class).where("boardId = ?", id).first() == null) {
			this.db.remove(board);
		} else {
			throw new ApiException(ApiError.OPERATION_FAILED, "board", "Cannot delete non-empty board.");
		}
	}

	@Transactional
	public void sortBoards(List<Long> ids) {
		List<Board> boards = getBoards();
		sortEntities(boards, ids);
	}

	public PagedResults<Topic> getTopics(Board board, int pageIndex) {
		PagedResults<Topic> result = null;
		if (pageIndex == 1) {
			result = this.redisService.get(KEY_TOPICS_FIRST_PAGE + board.id, TYPE_PAGE_RESULTS_TOPIC);
		}
		if (result == null) {
			result = this.db.from(Topic.class).where("boardId = ?", board.id).list(pageIndex, ITEMS_PER_PAGE);
			if (pageIndex == 1) {
				this.redisService.set(KEY_TOPICS_FIRST_PAGE + board.id, result, CACHE_TOPICS_SECONDS);
			}
		}
		return result;
	}

	public PagedResults<Reply> getReplies(Topic topic, int pageIndex) {
		// total = 1 + replies:
		int totalItems = 1 + this.db.from(Reply.class).where("topicId = ?", topic.id).count();
		int totalPages = totalItems / ITEMS_PER_PAGE + (totalItems % ITEMS_PER_PAGE > 0 ? 1 : 0);
		Page page = new Page(pageIndex, ITEMS_PER_PAGE, totalPages, totalItems);
		List<Reply> list = List.of();
		if (totalItems > 1) {
			// if page index is 1: offset = 0, items = 9,
			// else: offset = pageIndex * pageSize - 1
			int offset = pageIndex == 1 ? 0 : (pageIndex - 1) * ITEMS_PER_PAGE - 1;
			int items = pageIndex == 1 ? ITEMS_PER_PAGE - 1 : ITEMS_PER_PAGE;
			list = this.db.from(Reply.class).where("topicId = ?", topic.id).orderBy("id").limit(offset, items).list();
		}
		return new PagedResults<>(page, list);
	}

	@Transactional
	public Topic createTopic(User user, Board board, RefType refType, Long refId, TopicBean bean) {
		Topic topic = new Topic();
		topic.boardId = board.id;
		topic.content = checkText(markdown.ugcToHtml(checkText(bean.content)));
		topic.name = checkName(bean.name);
		topic.refId = refId;
		topic.refType = refType;
		topic.userId = user.id;
		this.db.insert(topic);
		this.db.updateSql(this.sqlUpdateBoardIncTopicNumber, topic.boardId);
		return topic;
	}

	@Transactional
	public void deleteTopic(User user, Long id) {
		Topic topic = getTopicById(id);
		super.checkPermission(user, topic.userId);
		this.db.remove(topic);
		this.db.updateSql(this.sqlDeleteReplies, id);
		this.db.updateSql(this.sqlUpdateBoardDecTopicNumber, topic.boardId);
	}

	public Topic getTopicById(Long id) {
		Topic topic = this.db.fetch(Topic.class, id);
		if (topic == null) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "topic", "Topic not exist.");
		}
		return topic;
	}

	public Reply getReplyById(Long id) {
		Reply reply = this.db.fetch(Reply.class, id);
		if (reply == null) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "reply", "Reply not exist.");
		}
		return reply;
	}

	@Transactional
	public Reply createReply(User user, Topic topic, ReplyBean bean) {
		Reply reply = new Reply();
		reply.userId = user.id;
		reply.topicId = topic.id;
		reply.content = checkText(markdown.ugcToHtml(checkText(bean.content)));
		this.db.insert(reply);
		this.db.updateSql(this.sqlUpdateTopicIncReplyNumber, reply.topicId);
		return reply;
	}

	@Transactional
	public void deleteReply(User user, Long id) {
		Reply reply = getReplyById(id);
		super.checkPermission(user, reply.userId);
		this.db.remove(reply);
		this.db.updateSql(this.sqlUpdateTopicDecReplyNumber, reply.topicId);
	}

	static final TypeReference<PagedResults<Topic>> TYPE_PAGE_RESULTS_TOPIC = new TypeReference<>() {
	};
}
