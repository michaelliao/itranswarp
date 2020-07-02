package com.itranswarp.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itranswarp.bean.AdMaterialBean;
import com.itranswarp.bean.AdPeriodBean;
import com.itranswarp.bean.AdSlotBean;
import com.itranswarp.bean.ArticleBean;
import com.itranswarp.bean.AttachmentBean;
import com.itranswarp.bean.BoardBean;
import com.itranswarp.bean.CategoryBean;
import com.itranswarp.bean.LinkBean;
import com.itranswarp.bean.NavigationBean;
import com.itranswarp.bean.ReplyBean;
import com.itranswarp.bean.SinglePageBean;
import com.itranswarp.bean.SortBean;
import com.itranswarp.bean.TopicBean;
import com.itranswarp.bean.WikiBean;
import com.itranswarp.bean.WikiPageBean;
import com.itranswarp.bean.WikiPageMoveBean;
import com.itranswarp.bean.setting.Follow;
import com.itranswarp.bean.setting.Security;
import com.itranswarp.bean.setting.Snippet;
import com.itranswarp.bean.setting.Website;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.RefType;
import com.itranswarp.enums.Role;
import com.itranswarp.model.AdMaterial;
import com.itranswarp.model.AdPeriod;
import com.itranswarp.model.AdSlot;
import com.itranswarp.model.Article;
import com.itranswarp.model.Attachment;
import com.itranswarp.model.Board;
import com.itranswarp.model.Category;
import com.itranswarp.model.Link;
import com.itranswarp.model.Navigation;
import com.itranswarp.model.Reply;
import com.itranswarp.model.SinglePage;
import com.itranswarp.model.Topic;
import com.itranswarp.model.User;
import com.itranswarp.model.Wiki;
import com.itranswarp.model.WikiPage;
import com.itranswarp.service.BoardService.TopicWithReplies;
import com.itranswarp.warpdb.Page;
import com.itranswarp.warpdb.PagedResults;
import com.itranswarp.web.filter.HttpContext;
import com.itranswarp.web.support.RoleWith;

@RestController
@RequestMapping("/api")
public class ApiController extends AbstractController {

	@Value("${spring.security.anti-spam.lock-days:3650}")
	int lockDaysForSpam = 0;

	///////////////////////////////////////////////////////////////////////////////////////////////
	// ad
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/adSlots")
	@RoleWith(Role.ADMIN)
	public Map<String, List<AdSlot>> adSlotList() {
		return Map.of(RESULTS, this.adService.getAdSlots());
	}

	@PostMapping("/adSlots")
	@RoleWith(Role.ADMIN)
	public AdSlot adSlotCreate(@RequestBody AdSlotBean bean) {
		AdSlot as = this.adService.createAdSlot(bean);
		this.adService.deleteAdInfoFromCache();
		return as;
	}

	@GetMapping("/adSlots/" + ID)
	@RoleWith(Role.ADMIN)
	public AdSlot adSlot(@PathVariable("id") long id) {
		return this.adService.getById(id);
	}

	@PostMapping("/adSlots/" + ID)
	@RoleWith(Role.ADMIN)
	public AdSlot adSlotUpdate(@PathVariable("id") long id, @RequestBody AdSlotBean bean) {
		AdSlot as = this.adService.updateAdSlot(id, bean);
		this.adService.deleteAdInfoFromCache();
		return as;
	}

	@PostMapping("/adSlots/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> adSlotDelete(@PathVariable("id") long id) {
		this.adService.deleteAdSlot(id);
		this.adService.deleteAdInfoFromCache();
		return API_RESULT_TRUE;
	}

	@GetMapping("/adPeriods")
	@RoleWith(Role.ADMIN)
	public Map<String, List<AdPeriod>> adPeriodList() {
		return Map.of(RESULTS, this.adService.getAdPeriods());
	}

	@GetMapping("/adPeriods/" + ID)
	@RoleWith(Role.ADMIN)
	public AdPeriod adPeriodGet(@PathVariable("id") long id) {
		return this.adService.getAdPeriodById(id);
	}

	@PostMapping("/adPeriods")
	@RoleWith(Role.ADMIN)
	public AdPeriod adPeriodCreate(@RequestBody AdPeriodBean bean) {
		AdPeriod ap = this.adService.createAdPeriod(bean);
		this.adService.deleteAdInfoFromCache();
		return ap;
	}

	@PostMapping("/adPeriods/" + ID)
	@RoleWith(Role.ADMIN)
	public AdPeriod adPeriodUpdate(@PathVariable("id") long id, @RequestBody AdPeriodBean bean) {
		AdPeriod ap = this.adService.updateAdPeriod(id, bean);
		this.adService.deleteAdInfoFromCache();
		return ap;
	}

	@PostMapping("/adPeriods/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> adPeriodDelete(@PathVariable("id") long id) {
		this.adService.deleteAdPeriod(id);
		this.adService.deleteAdInfoFromCache();
		return API_RESULT_TRUE;
	}

	@GetMapping("/adMaterials")
	@RoleWith(Role.ADMIN)
	public PagedResults<AdMaterial> adMaterialList(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return this.adService.getAdMaterials(pageIndex);
	}

	@PostMapping("/adPeriods/" + ID + "/adMaterials")
	@RoleWith(Role.SPONSOR)
	public AdMaterial adMaterialCreate(@PathVariable("id") long id, @RequestBody AdMaterialBean bean) {
		User user = HttpContext.getRequiredCurrentUser();
		AdPeriod adPeriod = this.adService.getAdPeriodById(id);
		if (adPeriod.userId != user.id) {
			throw new ApiException(ApiError.PERMISSION_DENIED, null, "Permission denied.");
		}
		AdMaterial am = this.adService.createAdMaterial(user, adPeriod, bean);
		this.adService.deleteAdInfoFromCache();
		return am;
	}

	@PostMapping("/adMaterials/" + ID + "/delete")
	@RoleWith(Role.SPONSOR)
	public Map<String, Boolean> adMaterialDelete(@PathVariable("id") long id) {
		User user = HttpContext.getRequiredCurrentUser();
		this.adService.deleteAdMaterial(user, id);
		this.adService.deleteAdInfoFromCache();
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// article
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/articles/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public ArticleWithContent article(@PathVariable("id") long id) {
		Article article = this.articleService.getById(id);
		String content = this.textService.getById(article.textId).content;
		return new ArticleWithContent(article, content);
	}

	@GetMapping("/articles")
	@RoleWith(Role.CONTRIBUTOR)
	public PagedResults<Article> articles(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return this.articleService.getArticles(pageIndex);
	}

	@PostMapping("/articles")
	@RoleWith(Role.CONTRIBUTOR)
	public Article articleCreate(@RequestBody ArticleBean bean) {
		Article article = this.articleService.createArticle(HttpContext.getRequiredCurrentUser(), bean);
		this.articleService.deleteArticlesFromCache(article.categoryId);
		return article;
	}

	@PostMapping("/articles/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public Article articleUpdate(@PathVariable("id") long id, @RequestBody ArticleBean bean) {
		Article article = this.articleService.updateArticle(HttpContext.getRequiredCurrentUser(), id, bean);
		this.articleService.deleteArticlesFromCache(article.categoryId);
		return article;
	}

	@PostMapping("/articles/" + ID + "/delete")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, Boolean> articleDelete(@PathVariable("id") long id) {
		Article article = this.articleService.deleteArticle(HttpContext.getRequiredCurrentUser(), id);
		this.articleService.deleteArticlesFromCache(article.categoryId);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// attachment
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/attachments")
	@RoleWith(Role.CONTRIBUTOR)
	public PagedResults<Attachment> attachments(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return this.attachmentService.getAttachments(pageIndex);
	}

	@GetMapping("/attachments/" + ID)
	@RoleWith(Role.SPONSOR)
	public Attachment attachment(@PathVariable("id") long id) {
		return this.attachmentService.getById(id);
	}

	@PostMapping("/attachments")
	@RoleWith(Role.SPONSOR)
	public Attachment attachmentCreate(@RequestBody AttachmentBean bean) {
		return this.attachmentService.createAttachment(HttpContext.getRequiredCurrentUser(), bean);
	}

	@PostMapping("/attachments/" + ID + "/delete")
	@RoleWith(Role.SPONSOR)
	public Map<String, Boolean> attachmentDelete(@PathVariable("id") long id) {
		this.attachmentService.deleteAttachment(HttpContext.getRequiredCurrentUser(), id);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// board
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/boards")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<Board>> boards() {
		return Map.of(RESULTS, this.boardService.getBoards());
	}

	@GetMapping("/boards/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public Board board(@PathVariable("id") long id) {
		return this.boardService.getById(id);
	}

	@PostMapping("/boards/sort")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> boardsSort(@RequestBody SortBean bean) {
		this.boardService.sortBoards(bean.ids);
		this.boardService.deleteBoardsFromCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/boards")
	@RoleWith(Role.ADMIN)
	public Board boardCreate(@RequestBody BoardBean bean) {
		Board board = this.boardService.createBoard(bean);
		this.boardService.deleteBoardsFromCache();
		return board;
	}

	@PostMapping("/boards/" + ID)
	@RoleWith(Role.ADMIN)
	public Board boardUpdate(@PathVariable("id") long id, @RequestBody BoardBean bean) {
		Board board = this.boardService.updateBoard(id, bean);
		this.boardService.deleteBoardsFromCache();
		return board;
	}

	@PostMapping("/boards/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> boardDelete(@PathVariable("id") long id) {
		this.boardService.deleteBoard(id);
		this.boardService.deleteBoardFromCache(id);
		this.boardService.deleteTopicsFromCache(id);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// category
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/categories")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<Category>> categories() {
		return Map.of(RESULTS, this.articleService.getCategories());
	}

	@GetMapping("/categories/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public Category category(@PathVariable("id") long id) {
		return this.articleService.getCategoryById(id);
	}

	@PostMapping("/categories/sort")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> categoriesSort(@RequestBody SortBean bean) {
		this.articleService.sortCategories(bean.ids);
		this.articleService.deleteCategoriesFromCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/categories")
	@RoleWith(Role.ADMIN)
	public Category categoryCreate(@RequestBody CategoryBean bean) {
		return this.articleService.createCategory(bean);
	}

	@PostMapping("/categories/" + ID)
	@RoleWith(Role.ADMIN)
	public Category categoryUpdate(@PathVariable("id") long id, @RequestBody CategoryBean bean) {
		Category category = this.articleService.updateCategory(id, bean);
		this.articleService.deleteCategoryFromCache(id);
		return category;
	}

	@PostMapping("/categories/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> categoryDelete(@PathVariable("id") long id) {
		this.articleService.deleteCategory(id);
		this.articleService.deleteCategoryFromCache(id);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// link
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/links")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<Link>> links() {
		return Map.of(RESULTS, this.linkService.getLinks());
	}

	@GetMapping("/links/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public Link linkGet(@PathVariable("id") long id) {
		return this.linkService.getById(id);
	}

	@PostMapping("/links")
	@RoleWith(Role.ADMIN)
	public Link linkCreate(@RequestBody LinkBean bean) {
		Link link = this.linkService.createLink(bean);
		this.linkService.updateLinksCache();
		return link;
	}

	@PostMapping("/links/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> linkDelete(@PathVariable("id") long id) {
		this.linkService.deleteLink(id);
		this.linkService.updateLinksCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/links/" + ID)
	@RoleWith(Role.ADMIN)
	public Link linkUpdate(@PathVariable("id") long id, @RequestBody LinkBean bean) {
		Link link = this.linkService.updateLink(id, bean);
		this.linkService.updateLinksCache();
		return link;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// navigation
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/navigations")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<Navigation>> navigations() {
		return Map.of(RESULTS, this.navigationService.getNavigations());
	}

	@GetMapping("/navigations/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public Navigation navigationGet(@PathVariable("id") long id) {
		return this.navigationService.getById(id);
	}

	@GetMapping("/navigations/urls")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<NavigationMenu>> navigationUrls() {
		List<NavigationMenu> list = new ArrayList<>();
		this.articleService.getCategories().forEach(c -> {
			list.add(new NavigationMenu(c.name, "/category/" + c.id));
		});
		this.wikiService.getWikis().forEach(w -> {
			list.add(new NavigationMenu(w.name, "/wiki/" + w.id));
		});
		this.singlePageService.getAll().forEach(p -> {
			list.add(new NavigationMenu(p.name, "/single/" + p.id));
		});
		list.add(new NavigationMenu("Discuss", "/discuss"));
		list.add(new NavigationMenu("Custom", "http://"));
		return Map.of(RESULTS, list);
	}

	@PostMapping("/navigations")
	@RoleWith(Role.ADMIN)
	public Navigation navigationCreate(@RequestBody NavigationBean bean) {
		Navigation nav = this.navigationService.createNavigation(bean);
		this.navigationService.removeNavigationsFromCache();
		return nav;
	}

	@PostMapping("/navigations/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> navigationDelete(@PathVariable("id") long id) {
		this.navigationService.deleteNavigation(id);
		this.navigationService.removeNavigationsFromCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/navigations/sort")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> navigationsSort(@RequestBody SortBean bean) {
		this.navigationService.sortNavigations(bean.ids);
		this.navigationService.removeNavigationsFromCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/navigations/" + ID)
	@RoleWith(Role.ADMIN)
	public Navigation navigationUpdate(@PathVariable("id") long id, @RequestBody NavigationBean bean) {
		Navigation navigation = this.navigationService.updateNavigation(id, bean);
		this.navigationService.removeNavigationsFromCache();
		return navigation;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// search
	///////////////////////////////////////////////////////////////////////////////////////////////

	private String KEY_REINDEX_STATUS = "__reindex_status__";
	private String KEY_REINDEX_PROGRESS = "__reindex_progress__";

	@PostMapping("/search/reindex")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> searchReindex() throws Exception {
		if (this.redisService.get(KEY_REINDEX_STATUS) != null) {
			throw new ApiException(ApiError.OPERATION_FAILED, "reindex", "Reindex is in progress...");
		}
		searchReindexAsync();
		return API_RESULT_TRUE;
	}

	private void searchReindexAsync() throws Exception {
		logger.warn("START reindex all documents...");
		int indexedDocuments = 0;
		this.redisService.set(KEY_REINDEX_PROGRESS, indexedDocuments);
		this.redisService.set(KEY_REINDEX_STATUS, "indexing");
		try {
			logger.warn("reindex all articles...");
			for (int i = 1; i < 100000; i++) {
				PagedResults<Article> pr = this.articleService.getArticles(i);
				if (pr.results.isEmpty()) {
					break;
				}
				this.searcher.indexArticles(pr.results.stream().toArray(Article[]::new));
				indexedDocuments += pr.results.size();
				this.redisService.set(KEY_REINDEX_PROGRESS, indexedDocuments);
			}
			logger.warn("END reindex all documents.");
		} finally {
			this.redisService.del(KEY_REINDEX_STATUS);
		}
	}

	@GetMapping("/search/reindex")
	@RoleWith(Role.ADMIN)
	public Map<String, Object> searchReindexStatus() {
		if (this.redisService.get(KEY_REINDEX_STATUS) == null) {
			return Map.of("process", Boolean.FALSE);
		}
		var n = this.redisService.get(KEY_REINDEX_PROGRESS, Integer.class);
		return Map.of("process", Boolean.TRUE, "progress", n);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// reply
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/replies")
	@RoleWith(Role.CONTRIBUTOR)
	public PagedResults<Reply> replyList(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return this.boardService.getReplies(pageIndex);
	}

	@PostMapping("/topics/" + ID + "/replies")
	@RoleWith(Role.SUBSCRIBER)
	public Reply replyCreate(@PathVariable("id") long id, @RequestBody ReplyBean bean) {
		User user = HttpContext.getRequiredCurrentUser();
		Topic topic = this.boardService.getTopicById(id);
		Reply reply = null;
		try {
			reply = this.boardService.createReply(user, topic, bean);
		} catch (ApiException e) {
			if (e.error == ApiError.SECURITY_ANTI_SPAM && lockDaysForSpam > 0) {
				this.userService.lockUser(user, lockDaysForSpam);
			}
			throw e;
		}
		this.boardService.deleteBoardFromCache(topic.boardId);
		this.boardService.deleteTopicsFromCache(topic.boardId);
		return reply;
	}

	@PostMapping("/replies/" + ID + "/delete")
	@RoleWith(Role.EDITOR)
	public Map<String, Boolean> replyDelete(@PathVariable("id") long id) {
		this.boardService.deleteReply(HttpContext.getRequiredCurrentUser(), id);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// settings
	///////////////////////////////////////////////////////////////////////////////////////////////

	@PostMapping("/setting/website")
	public Map<String, Boolean> settingWebsiteUpdate(@RequestBody Website bean) {
		this.settingService.setWebsite(bean);
		this.settingService.deleteWebsiteFromCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/setting/snippet")
	public Map<String, Boolean> settingSnippetUpdate(@RequestBody Snippet bean) {
		this.settingService.setSnippet(bean);
		this.settingService.deleteSnippetFromCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/setting/follow")
	public Map<String, Boolean> settingFollowUpdate(@RequestBody Follow bean) {
		this.settingService.setFollow(bean);
		this.settingService.deleteFollowFromCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/setting/security")
	public Map<String, Boolean> settingSecurityUpdate(@RequestBody Security bean) {
		this.settingService.setSecurity(bean);
		this.settingService.deleteSecurityFromCache();
		super.antiSpamService.setSpamKeywords(bean.getSpamKeywordsAsList());
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// single page
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/singlePages")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<SinglePage>> singlePages() {
		return Map.of(RESULTS, this.singlePageService.getAll());
	}

	@GetMapping("/singlePages/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public SinglePageWithContent singlePage(@PathVariable("id") long id) {
		SinglePage sp = this.singlePageService.getById(id);
		String content = this.textService.getById(sp.textId).content;
		return new SinglePageWithContent(sp, content);
	}

	@PostMapping("/singlePages")
	@RoleWith(Role.ADMIN)
	public SinglePage singlePageCreate(@RequestBody SinglePageBean bean) {
		return this.singlePageService.createSinglePage(bean);
	}

	@PostMapping("/singlePages/" + ID)
	@RoleWith(Role.ADMIN)
	public SinglePage singlePageUpdate(@PathVariable("id") long id, @RequestBody SinglePageBean bean) {
		SinglePage sp = this.singlePageService.updateSinglePage(id, bean);
		this.singlePageService.deleteSinglePageFromCache(id);
		return sp;
	}

	@PostMapping("/singlePages/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> singlePageDelete(@PathVariable("id") long id) {
		this.singlePageService.deleteSinglePage(id);
		this.singlePageService.deleteSinglePageFromCache(id);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// topic
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/topics")
	@RoleWith(Role.CONTRIBUTOR)
	public PagedResults<Topic> topics(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return this.boardService.getTopics(pageIndex);
	}

	@GetMapping("/ref/" + ID + "/topics")
	public Map<String, List<TopicWithReplies>> topicsByRefId(@PathVariable("id") long refId) {
		return Map.of(RESULTS, this.boardService.getTopicsByRefId(refId));
	}

	@PostMapping("/comments/{tag}")
	@RoleWith(Role.SUBSCRIBER)
	public Topic topicCreateByRefType(@PathVariable("tag") String tag, @RequestBody TopicBean bean) {
		Board board = this.boardService.getBoardByTagFromCache(tag);
		if (board.locked) {
			throw new ApiException(ApiError.OPERATION_FAILED, "board", "Board is locked.");
		}
		if (bean.refId == 0) {
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
		default:
			throw new ApiException(ApiError.PARAMETER_INVALID, "refType", "Unsupported refType: " + bean.refType);
		}
		Topic topic = null;
		User user = HttpContext.getRequiredCurrentUser();
		try {
			topic = this.boardService.createTopic(user, board, bean);
		} catch (ApiException e) {
			if (e.error == ApiError.SECURITY_ANTI_SPAM && lockDaysForSpam > 0) {
				this.userService.lockUser(user, lockDaysForSpam);
			}
			throw e;
		}
		this.boardService.deleteBoardFromCache(topic.boardId);
		this.boardService.deleteTopicsFromCache(topic.boardId);
		return topic;
	}

	@PostMapping("/boards/" + ID + "/topics")
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
		Topic topic = null;
		User user = HttpContext.getRequiredCurrentUser();
		try {
			topic = this.boardService.createTopic(user, board, bean);
		} catch (ApiException e) {
			if (e.error == ApiError.SECURITY_ANTI_SPAM && lockDaysForSpam > 0) {
				this.userService.lockUser(user, lockDaysForSpam);
			}
			throw e;
		}
		this.boardService.deleteBoardFromCache(topic.boardId);
		this.boardService.deleteTopicsFromCache(topic.boardId);
		return topic;
	}

	@PostMapping("/topics/" + ID + "/delete")
	@RoleWith(Role.EDITOR)
	public Map<String, Boolean> topicDelete(@PathVariable("id") long id) {
		Topic topic = this.boardService.deleteTopic(HttpContext.getRequiredCurrentUser(), id);
		this.boardService.deleteBoardFromCache(topic.boardId);
		this.boardService.deleteTopicsFromCache(topic.boardId);
		return API_RESULT_TRUE;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// user
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/users")
	@RoleWith(Role.CONTRIBUTOR)
	public PagedResults<User> users(@RequestParam(value = "q", defaultValue = "") String q,
			@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		if (q.isBlank()) {
			return this.userService.getUsers(pageIndex);
		}
		try {
			long id = Long.parseLong(q);
			User user = this.userService.getById(id);
			return new PagedResults<>(new Page(1, 10, 1, 1), List.of(user));
		} catch (NumberFormatException e) {
			// ignore
		} catch (ApiException e) {
			// ignore
		}
		List<User> users = this.userService.searchUsers(q);
		return new PagedResults<>(new Page(1, users.size(), 1, users.size()), users);
	}

	@GetMapping("/users/ids")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<Long, User> usersByIds(@RequestParam("id") long[] ids) {
		List<User> users = this.userService.getUsersByIds(ids);
		return users.stream().collect(Collectors.toMap(u -> u.id, u -> u));
	}

	@PostMapping("/users/" + ID + "/role/{role}")
	@RoleWith(Role.ADMIN)
	public User userUpdateRole(@PathVariable("id") long id, @PathVariable("role") Role role) {
		return this.userService.updateUserRole(id, role);
	}

	@PostMapping("/users/" + ID + "/lock/{timestamp}")
	@RoleWith(Role.ADMIN)
	public User userUpdateLock(@PathVariable("id") long id, @PathVariable("timestamp") long timestamp) {
		if (timestamp < 0) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "timestamp", "Invalid timestamp.");
		}
		return this.userService.updateUserLockedUntil(id, timestamp);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// wiki and wiki page
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/wikis")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<Wiki>> wikis() {
		return Map.of(RESULTS, this.wikiService.getWikis());
	}

	@GetMapping("/wikis/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public WikiWithContent wiki(@PathVariable("id") long id) {
		Wiki wiki = this.wikiService.getById(id);
		String content = this.textService.getById(wiki.textId).content;
		return new WikiWithContent(wiki, content);
	}

	@GetMapping("/wikis/" + ID + "/tree")
	@RoleWith(Role.CONTRIBUTOR)
	public Wiki wikiTree(@PathVariable("id") long id) {
		return this.wikiService.getWikiTree(id);
	}

	@PostMapping("/wikis")
	@RoleWith(Role.EDITOR)
	public Wiki wikiCreate(@RequestBody WikiBean bean) {
		Wiki wiki = this.wikiService.createWiki(HttpContext.getRequiredCurrentUser(), bean);
		this.wikiService.removeWikiFromCache(wiki.id);
		return wiki;
	}

	@PostMapping("/wikis/" + ID)
	@RoleWith(Role.EDITOR)
	public Wiki wikiUpdate(@PathVariable("id") long id, @RequestBody WikiBean bean) {
		Wiki wiki = this.wikiService.updateWiki(HttpContext.getRequiredCurrentUser(), id, bean);
		this.wikiService.removeWikiFromCache(id);
		return wiki;
	}

	@PostMapping("/wikis/" + ID + "/delete")
	@RoleWith(Role.EDITOR)
	public Map<String, Boolean> wikiDelete(@PathVariable("id") long id) {
		this.wikiService.deleteWiki(HttpContext.getRequiredCurrentUser(), id);
		this.wikiService.removeWikiFromCache(id);
		return API_RESULT_TRUE;
	}

	@PostMapping("/wikis/" + ID + "/wikiPages")
	@RoleWith(Role.EDITOR)
	public WikiPage wikiPageCreate(@PathVariable("id") long id, @RequestBody WikiPageBean bean) {
		Wiki wiki = this.wikiService.getById(id);
		WikiPage wikiPage = this.wikiService.createWikiPage(HttpContext.getRequiredCurrentUser(), wiki, bean);
		this.wikiService.removeWikiFromCache(id);
		return wikiPage;
	}

	@GetMapping("/wikiPages/" + ID)
	@RoleWith(Role.EDITOR)
	public WikiPageWithContent wikiPage(@PathVariable("id") long id) {
		WikiPage wp = this.wikiService.getWikiPageById(id);
		String content = this.textService.getById(wp.textId).content;
		return new WikiPageWithContent(wp, content);
	}

	@PostMapping("/wikiPages/" + ID)
	@RoleWith(Role.EDITOR)
	public WikiPage wikiPageUpdate(@PathVariable("id") long id, @RequestBody WikiPageBean bean) {
		WikiPage wikiPage = this.wikiService.updateWikiPage(HttpContext.getRequiredCurrentUser(), id, bean);
		this.wikiService.removeWikiFromCache(wikiPage.wikiId);
		return wikiPage;
	}

	@PostMapping("/wikiPages/" + ID + "/move")
	@RoleWith(Role.EDITOR)
	public WikiPage wikiPageUpdate(@PathVariable("id") long wikiPageId, @RequestBody WikiPageMoveBean bean) {
		bean.validate(true);
		WikiPage wikiPage = this.wikiService.moveWikiPage(HttpContext.getRequiredCurrentUser(), wikiPageId,
				bean.parentId, bean.displayIndex);
		this.wikiService.removeWikiFromCache(wikiPage.wikiId);
		return wikiPage;
	}

	@PostMapping("/wikiPages/" + ID + "/delete")
	@RoleWith(Role.EDITOR)
	public Map<String, Boolean> wikiPageDelete(@PathVariable("id") long id) {
		WikiPage wikiPage = this.wikiService.deleteWikiPage(HttpContext.getRequiredCurrentUser(), id);
		this.wikiService.removeWikiFromCache(wikiPage.wikiId);
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

	public static class NavigationMenu {

		public String name;
		public String url;

		public NavigationMenu(String name, String url) {
			this.name = name;
			this.url = url;
		}
	}

	private static final String RESULTS = "results";

	private static final Map<String, Boolean> API_RESULT_TRUE = Map.of("result", Boolean.TRUE);

}
