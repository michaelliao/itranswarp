package com.itranswarp.web.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itranswarp.bean.ArticleBean;
import com.itranswarp.bean.AttachmentBean;
import com.itranswarp.bean.BoardBean;
import com.itranswarp.bean.CategoryBean;
import com.itranswarp.bean.NavigationBean;
import com.itranswarp.bean.SinglePageBean;
import com.itranswarp.bean.SortBean;
import com.itranswarp.bean.TopicBean;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.RefType;
import com.itranswarp.enums.Role;
import com.itranswarp.model.Article;
import com.itranswarp.model.Attachment;
import com.itranswarp.model.Board;
import com.itranswarp.model.Category;
import com.itranswarp.model.Navigation;
import com.itranswarp.model.Reply;
import com.itranswarp.model.SinglePage;
import com.itranswarp.model.Topic;
import com.itranswarp.model.User;
import com.itranswarp.model.Wiki;
import com.itranswarp.model.WikiPage;
import com.itranswarp.warpdb.PagedResults;
import com.itranswarp.web.filter.HttpContext;
import com.itranswarp.web.support.RoleWith;

@RestController
public class ApiController extends AbstractController {

	///////////////////////////////////////////////////////////////////////////////////////////////
	// category
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/api/categories")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<Category>> categories() {
		return Map.of("results", this.articleService.getCategories());
	}

	@GetMapping("/api/categories/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public Category category(@PathVariable("id") long id) {
		return this.articleService.getCategoryById(id);
	}

	@PostMapping("/api/categories/sort")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> categoriesSort(@RequestBody SortBean bean) {
		this.articleService.sortCategories(bean.ids);
		this.articleService.removeCategoriesFromCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/api/categories")
	@RoleWith(Role.ADMIN)
	public Category categoryCreate(@RequestBody CategoryBean bean) {
		return this.articleService.createCategory(bean);
	}

	@PostMapping("/api/categories/" + ID)
	@RoleWith(Role.ADMIN)
	public Category categoryUpdate(@PathVariable("id") long id, @RequestBody CategoryBean bean) {
		Category category = this.articleService.updateCategory(id, bean);
		this.articleService.removeCategoryFromCache(id);
		return category;
	}

	@PostMapping("/api/categories/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> categoryDelete(@PathVariable("id") long id) {
		this.articleService.deleteCategory(id);
		this.articleService.removeCategoryFromCache(id);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// article
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/api/articles/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public ArticleWithContent article(@PathVariable("id") long id) {
		Article article = this.articleService.getById(id);
		String content = this.textService.getById(article.textId).content;
		return new ArticleWithContent(article, content);
	}

	@GetMapping("/api/articles")
	@RoleWith(Role.CONTRIBUTOR)
	public PagedResults<Article> articles(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return this.articleService.getArticles(pageIndex);
	}

	@PostMapping("/api/articles")
	@RoleWith(Role.CONTRIBUTOR)
	public Article articleUpdate(@RequestBody ArticleBean bean) {
		return this.articleService.createArticle(HttpContext.getRequiredCurrentUser(), bean);
	}

	@PostMapping("/api/articles/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public Article articleUpdate(@PathVariable("id") long id, @RequestBody ArticleBean bean) {
		return this.articleService.updateArticle(HttpContext.getRequiredCurrentUser(), id, bean);
	}

	@PostMapping("/api/articles/" + ID + "/delete")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, Boolean> articleDelete(@PathVariable("id") long id) {
		this.articleService.deleteArticle(HttpContext.getRequiredCurrentUser(), id);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// single page
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/api/singlePages")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<SinglePage>> singlePages() {
		return Map.of("results", this.singlePageService.getAll());
	}

	@GetMapping("/api/singlePages/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public SinglePageWithContent singlePage(@PathVariable("id") long id) {
		SinglePage sp = this.singlePageService.getById(id);
		String content = this.textService.getById(sp.textId).content;
		return new SinglePageWithContent(sp, content);
	}

	@PostMapping("/api/singlePages")
	@RoleWith(Role.ADMIN)
	public SinglePage singlePageCreate(@RequestBody SinglePageBean bean) {
		return this.singlePageService.createSinglePage(bean);
	}

	@PostMapping("/api/singlePages/" + ID)
	@RoleWith(Role.ADMIN)
	public SinglePage singlePageUpdate(@PathVariable("id") long id, @RequestBody SinglePageBean bean) {
		SinglePage sp = this.singlePageService.updateSinglePage(id, bean);
		this.singlePageService.deleteSinglePageFromCache(id);
		return sp;
	}

	@PostMapping("/api/singlePages/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> singlePageDelete(@PathVariable("id") long id) {
		this.singlePageService.deleteSinglePage(id);
		this.singlePageService.deleteSinglePageFromCache(id);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// board
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/api/boards")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<Board>> boards() {
		return Map.of("results", this.boardService.getBoards());
	}

	@GetMapping("/api/boards/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public Board board(@PathVariable("id") long id) {
		return this.boardService.getById(id);
	}

	@PostMapping("/api/boards/sort")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> boardsSort(@RequestBody SortBean bean) {
		this.boardService.sortBoards(bean.ids);
		this.boardService.deleteBoardsFromCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/api/boards")
	@RoleWith(Role.ADMIN)
	public Board boardCreate(@RequestBody BoardBean bean) {
		return this.boardService.createBoard(bean);
	}

	@PostMapping("/api/boards/" + ID)
	@RoleWith(Role.ADMIN)
	public Board boardUpdate(@PathVariable("id") long id, @RequestBody BoardBean bean) {
		Board board = this.boardService.updateBoard(id, bean);
		this.boardService.deleteBoardsFromCache();
		return board;
	}

	@PostMapping("/api/boards/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> boardDelete(@PathVariable("id") long id) {
		this.boardService.deleteBoard(id);
		this.boardService.deleteBoardFromCache(id);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// topic
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/api/topics")
	@RoleWith(Role.CONTRIBUTOR)
	public PagedResults<Topic> topics(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return this.boardService.getTopics(pageIndex);
	}

	@PostMapping("/api/boards/" + ID + "/topics")
	@RoleWith(Role.SUBSCRIBER)
	public Topic topicCreate(@PathVariable("id") long id, @RequestBody TopicBean bean) {
		Board board = this.boardService.getBoardFromCache(id);
		if (board.locked) {
			throw new ApiException(ApiError.OPERATION_FAILED, "board", "Board is locked.");
		}
		if (bean.refType != RefType.NONE && bean.refId == 0) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "refId", "refId cannot be zero.");
		}
		switch (bean.refType) {
		case ARTICLE:
			this.articleService.getPublishedById(bean.refId);
			break;
		case WIKI:
			this.wikiService.getById(bean.refId);
			break;
		case WIKIPAGE:
			this.wikiService.getWikiPageById(bean.refId);
			break;
		case NONE:
			break;
		default:
			throw new ApiException(ApiError.PARAMETER_INVALID, "refType", "Unsupported refType: " + bean.refType);
		}
		Topic topic = this.boardService.createTopic(HttpContext.getRequiredCurrentUser(), board, bean);
		this.boardService.deleteBoardFromCache(topic.boardId);
		return topic;
	}

	@PostMapping("/api/topics/" + ID + "/delete")
	@RoleWith(Role.EDITOR)
	public Map<String, Boolean> topicDelete(@PathVariable("id") long id) {
		Topic topic = this.boardService.deleteTopic(HttpContext.getRequiredCurrentUser(), id);
		this.boardService.deleteBoardFromCache(topic.boardId);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// reply
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/api/replies")
	@RoleWith(Role.CONTRIBUTOR)
	public PagedResults<Reply> replies(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return this.boardService.getReplies(pageIndex);
	}

	@PostMapping("/api/replies/" + ID + "/delete")
	@RoleWith(Role.EDITOR)
	public Map<String, Boolean> replyDelete(@PathVariable("id") long id) {
		this.boardService.deleteReply(HttpContext.getRequiredCurrentUser(), id);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// user
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/api/users")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<Long, User> users(@RequestParam("id") long[] ids) {
		List<User> users = this.userService.getUsersByIds(ids);
		return users.stream().collect(Collectors.toMap(u -> u.id, u -> u));
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// attachment
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/api/attachments")
	@RoleWith(Role.CONTRIBUTOR)
	public PagedResults<Attachment> attachments(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return this.attachmentService.getAttachments(pageIndex);
	}

	@GetMapping("/api/attachments/" + ID)
	@RoleWith(Role.SPONSOR)
	public Attachment attachment(@PathVariable("id") long id) {
		return this.attachmentService.getById(id);
	}

	@PostMapping("/api/attachments")
	@RoleWith(Role.SPONSOR)
	public Attachment attachmentCreate(@RequestBody AttachmentBean bean) {
		return this.attachmentService.createAttachment(HttpContext.getRequiredCurrentUser(), bean);
	}

	@PostMapping("/api/attachments/" + ID + "/delete")
	@RoleWith(Role.SPONSOR)
	public Map<String, Boolean> attachmentDelete(@PathVariable("id") long id) {
		this.attachmentService.deleteAttachment(HttpContext.getRequiredCurrentUser(), id);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// navigation
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/api/navigations")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<Navigation>> navigations() {
		return Map.of("results", this.navigationService.getNavigations());
	}

	@PostMapping("/api/navigations/sort")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> navigationsSort(@RequestBody SortBean bean) {
		this.navigationService.sortNavigations(bean.ids);
		this.navigationService.removeNavigationsFromCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/api/navigations")
	@RoleWith(Role.ADMIN)
	public Navigation navigationCreate(@RequestBody NavigationBean bean) {
		return this.navigationService.createNavigation(bean);
	}

	@PostMapping("/api/navigations/" + ID)
	@RoleWith(Role.ADMIN)
	public Navigation navigationUpdate(@PathVariable("id") long id, @RequestBody NavigationBean bean) {
		Navigation navigation = this.navigationService.updateNavigation(id, bean);
		this.navigationService.removeNavigationsFromCache();
		return navigation;
	}

	@PostMapping("/api/navigations/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> navigationDelete(@PathVariable("id") long id) {
		this.navigationService.deleteNavigation(id);
		this.navigationService.removeNavigationsFromCache();
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// extended JSON-serializable bean
	///////////////////////////////////////////////////////////////////////////////////////////////

	public static class ArticleWithContent extends Article {

		public String content;

		public ArticleWithContent(Article article, String content) {
			copyPropertiesFrom(article);
			this.content = content;
		}

	}

	public static class SinglePageWithContent extends SinglePage {

		public String content;

		public SinglePageWithContent(SinglePage singlePage, String content) {
			copyPropertiesFrom(singlePage);
			this.content = content;
		}

	}

	public static class WikiWithContent extends Wiki {

		public String content;

		public WikiWithContent(Wiki wiki, String content) {
			copyPropertiesFrom(wiki);
			this.content = content;
		}

	}

	public static class WikiPageWithContent extends WikiPage {

		public String content;

		public WikiPageWithContent(WikiPage wikiPage, String content) {
			copyPropertiesFrom(wikiPage);
			this.content = content;
		}

	}

}
