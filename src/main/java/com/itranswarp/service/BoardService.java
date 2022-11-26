package com.itranswarp.service;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.itranswarp.bean.BoardBean;
import com.itranswarp.bean.ReplyBean;
import com.itranswarp.bean.TopicBean;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.markdown.Markdown;
import com.itranswarp.model.AbstractEntity;
import com.itranswarp.model.Board;
import com.itranswarp.model.Reply;
import com.itranswarp.model.Topic;
import com.itranswarp.model.User;
import com.itranswarp.warpdb.Page;
import com.itranswarp.warpdb.PagedResults;

@Component
public class BoardService extends AbstractDbService<Board> {

    @Autowired
    Markdown markdown;

    @Autowired
    AntiSpamService antiSpamService;

    static final String KEY_BOARDS = "__boards__";
    static final String KEY_TOPICS_FIRST_PAGE = "__topics__";
    static final String KEY_RECENT_TOPICS = "__recent_topics__";
    static final long CACHE_TOPICS_SECONDS = 3600;

    String sqlUpdateBoardIncTopicNumber;
    String sqlUpdateBoardDecTopicNumber;
    String sqlUpdateTopicUpdatedAtAndIncReplyNumber;
    String sqlUpdateTopicDecReplyNumber;
    String sqlDeleteReplies;

    @PostConstruct
    public void init() {
        String boardTable = this.db.getTable(Board.class);
        String topicTable = this.db.getTable(Topic.class);
        String replyTable = this.db.getTable(Reply.class);
        this.sqlUpdateBoardIncTopicNumber = "UPDATE " + boardTable + " SET topicNumber = topicNumber + 1 WHERE id = ?";
        this.sqlUpdateBoardDecTopicNumber = "UPDATE " + boardTable + " SET topicNumber = topicNumber - 1 WHERE id = ?";
        this.sqlUpdateTopicUpdatedAtAndIncReplyNumber = "UPDATE " + topicTable + " SET replyNumber = replyNumber + 1, updatedAt = ? WHERE id = ?";
        this.sqlUpdateTopicDecReplyNumber = "UPDATE " + topicTable + " SET replyNumber = replyNumber - 1 WHERE id = ?";

        this.sqlDeleteReplies = "DELETE FROM " + replyTable + " WHERE topicId = ?";
    }

    public Board getBoardByTag(String tag) {
        Board b = db.from(Board.class).where("tag = ?", tag).first();
        if (b == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "tag", "Board not found by tag: " + tag);
        }
        return b;
    }

    public Board getBoardFromCache(long id) {
        Board c = this.redisService.hget(KEY_BOARDS, id, Board.class);
        if (c == null) {
            c = getById(id);
            this.redisService.hset(KEY_BOARDS, id, c);
        }
        return c;
    }

    public void deleteBoardsFromCache() {
        this.redisService.del(KEY_BOARDS);
    }

    public void deleteBoardFromCache(long id) {
        this.redisService.hdel(KEY_BOARDS, id);
        this.redisService.del(KEY_TOPICS_FIRST_PAGE + id);
    }

    public List<Board> getBoards() {
        return this.db.from(Board.class).orderBy("displayOrder").list();
    }

    @Transactional
    public Board createBoard(BoardBean bean) {
        bean.validate(true);
        long maxDisplayOrder = getBoards().stream().mapToLong(c -> c.displayOrder).max().orElseGet(() -> 0);
        Board board = new Board();
        board.name = bean.name;
        board.description = bean.description;
        board.tag = bean.tag;
        board.displayOrder = maxDisplayOrder + 1;
        this.db.insert(board);
        return board;
    }

    @Transactional
    public Board updateBoard(long id, BoardBean bean) {
        bean.validate(false);
        Board board = this.getById(id);
        board.name = bean.name;
        board.description = bean.description;
        board.tag = bean.tag;
        this.db.update(board);
        return board;
    }

    @Transactional
    public void deleteBoard(long id) {
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

    public List<Topic> getRecentTopicsFromCache() {
        List<Topic> topics = this.redisService.get(KEY_RECENT_TOPICS, TYPE_LIST_TOPIC);
        if (topics == null) {
            topics = this.db.from(Topic.class).orderBy("updatedAt").desc().limit(25).list();
            this.redisService.set(KEY_RECENT_TOPICS, topics, CACHE_TOPICS_SECONDS);
        }
        return topics;
    }

    public int getRecentTopicCountByUser(long userId, long startTime) {
        return this.db.from(Topic.class).where("userId = ? AND createdAt >= ?", userId, startTime).count();
    }

    public void deleteTopicsFromCache(long boardId) {
        this.redisService.del(KEY_TOPICS_FIRST_PAGE + boardId);
        this.redisService.del(KEY_RECENT_TOPICS);
    }

    public PagedResults<Topic> getTopicsFromCache(Board board, int pageIndex) {
        PagedResults<Topic> result = null;
        if (pageIndex == 1) {
            result = this.redisService.get(KEY_TOPICS_FIRST_PAGE + board.id, TYPE_PAGE_RESULTS_TOPIC);
        }
        if (result == null) {
            result = getTopics(board, pageIndex);
            if (pageIndex == 1) {
                this.redisService.set(KEY_TOPICS_FIRST_PAGE + board.id, result, CACHE_TOPICS_SECONDS);
            }
        }
        return result;
    }

    public PagedResults<Topic> getTopics(Board board, int pageIndex) {
        return this.db.from(Topic.class).where("boardId = ?", board.id).orderBy("updatedAt").desc().orderBy("id").desc().list(pageIndex, ITEMS_PER_PAGE);
    }

    public List<TopicWithReplies> getTopicsByRefId(long refId) {
        List<Topic> topics = this.db.from(Topic.class).where("refId = ?", refId).orderBy("updatedAt").desc().orderBy("id").desc().limit(20).list();
        return topics.stream().map(topic -> {
            TopicWithReplies tw = new TopicWithReplies();
            tw.copyPropertiesFrom(topic);
            tw.replies = getRecentReplies(topic);
            return tw;
        }).collect(Collectors.toList());
    }

    public List<Topic> getTopicsByUser(long userId) {
        return this.db.from(Topic.class).where("userId = ?", userId).orderBy("updatedAt").desc().orderBy("id").desc().limit(20).list();
    }

    public PagedResults<Topic> getTopics(int pageIndex) {
        return this.db.from(Topic.class).orderBy("id").desc().list(pageIndex, ITEMS_PER_PAGE);
    }

    public PagedResults<Reply> getReplies(int pageIndex) {
        return this.db.from(Reply.class).orderBy("id").desc().list(pageIndex);
    }

    public List<Reply> getRecentReplies(Topic topic) {
        return this.db.from(Reply.class).where("topicId = ?", topic.id).orderBy("id").limit(10).list();
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

    public int getReplyPageIndex(long topicId, long replyId) {
        int offset = 2 + this.db.from(Reply.class).where("topicId = ? AND id < ?", topicId, replyId).count();
        return offset / ITEMS_PER_PAGE + (offset % ITEMS_PER_PAGE == 0 ? 0 : 1);
    }

    @Transactional
    public Topic createTopic(User user, Board board, TopicBean bean) {
        bean.validate(true);
        if (this.antiSpamService.isSpamText(bean.name) || this.antiSpamService.isSpamText(bean.content)) {
            throw new ApiException(ApiError.SECURITY_ANTI_SPAM, "content", "Spam detected.");
        }
        Topic topic = new Topic();
        topic.boardId = board.id;
        topic.content = markdown.ugcToHtml(bean.content, AbstractEntity.TEXT);
        topic.name = bean.name;
        topic.refId = bean.refId;
        topic.refType = bean.refType;
        topic.userId = user.id;
        topic.userName = user.name;
        topic.userImageUrl = user.imageUrl;
        this.db.insert(topic);
        this.db.updateSql(this.sqlUpdateBoardIncTopicNumber, topic.boardId);
        return topic;
    }

    @Transactional
    public Topic deleteTopic(User user, long id) {
        Topic topic = getTopicById(id);
        super.checkPermission(user, topic.userId);
        this.db.remove(topic);
        this.db.updateSql(this.sqlDeleteReplies, id);
        this.db.updateSql(this.sqlUpdateBoardDecTopicNumber, topic.boardId);
        return topic;
    }

    public Topic getTopicById(long id) {
        Topic topic = this.db.fetch(Topic.class, id);
        if (topic == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "topic", "Topic not exist.");
        }
        return topic;
    }

    public Reply getReplyById(long id) {
        Reply reply = this.db.fetch(Reply.class, id);
        if (reply == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "reply", "Reply not exist.");
        }
        return reply;
    }

    @Transactional
    public Reply createReply(User user, Topic topic, ReplyBean bean) {
        bean.validate(true);
        if (this.antiSpamService.isSpamText(bean.content)) {
            throw new ApiException(ApiError.SECURITY_ANTI_SPAM, "content", "Spam detected.");
        }
        Reply reply = new Reply();
        reply.userId = user.id;
        reply.userName = user.name;
        reply.userImageUrl = user.imageUrl;
        reply.topicId = topic.id;
        reply.content = markdown.ugcToHtml(bean.content, AbstractEntity.TEXT);
        this.db.insert(reply);
        this.db.updateSql(this.sqlUpdateTopicUpdatedAtAndIncReplyNumber, System.currentTimeMillis(), reply.topicId);
        return reply;
    }

    @Transactional
    public void deleteReply(User user, long id) {
        Reply reply = getReplyById(id);
        super.checkPermission(user, reply.userId);
        this.db.remove(reply);
        this.db.updateSql(this.sqlUpdateTopicDecReplyNumber, reply.topicId);
    }

    public static class TopicWithReplies extends Topic {

        public List<Reply> replies;

    }

    private static final TypeReference<List<Topic>> TYPE_LIST_TOPIC = new TypeReference<>() {
    };

    private static final TypeReference<PagedResults<Topic>> TYPE_PAGE_RESULTS_TOPIC = new TypeReference<>() {
    };

}
