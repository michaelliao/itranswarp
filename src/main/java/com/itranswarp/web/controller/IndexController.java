package com.itranswarp.web.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.itranswarp.markdown.Markdown;
import com.itranswarp.model.Article;
import com.itranswarp.model.Board;
import com.itranswarp.model.Category;
import com.itranswarp.model.Reply;
import com.itranswarp.model.SinglePage;
import com.itranswarp.model.Topic;
import com.itranswarp.model.User;
import com.itranswarp.model.Wiki;
import com.itranswarp.model.WikiPage;
import com.itranswarp.service.ViewService;
import com.itranswarp.warpdb.Page;
import com.itranswarp.warpdb.PagedResults;
import com.itranswarp.web.filter.HttpContext;

@Controller
public class IndexController extends AbstractMvcController {

	@Autowired
	ViewService viewService;

	@Autowired
	Markdown markdown;

	@GetMapping("/")
	public ModelAndView index() {
		List<Article> recentArticles = this.articleService.getPublishedArticles(10);
		return prepareModelAndView("index.html", Map.of("recentArticles", recentArticles));
	}

	@GetMapping("/category/" + ID)
	public ModelAndView category(@PathVariable("id") String id,
			@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		Category category = articleService.getCategoryFromCache(id);
		PagedResults<Article> pr = articleService.getPublishedArticles(category, pageIndex);
		List<Article> articles = pr.getResults();
		if (!articles.isEmpty()) {
			long[] views = this.viewService.getViews(articles.stream().map(a -> a.id).toArray(String[]::new));
			int n = 0;
			for (Article article : articles) {
				article.views += views[n];
				n++;
			}
		}
		return prepareModelAndView("category.html",
				Map.of("category", category, "page", pr.page, "articles", articles));
	}

	@GetMapping("/article/" + ID)
	public ModelAndView article(@PathVariable("id") String id) {
		Article article = articleService.getPublishedById(id);
		article.views += viewService.increaseArticleViews(id);
		Category category = articleService.getCategoryFromCache(article.categoryId);
		User author = userService.getUserFromCache(article.userId);
		String content = textService.getHtmlFromCache(article.textId);
		return prepareModelAndView("article.html",
				Map.of("article", article, "author", author, "category", category, "content", content));
	}

	@GetMapping("/wiki/" + ID)
	public ModelAndView wiki(@PathVariable("id") String id) {
		Wiki wiki = wikiService.getWikiTreeFromCache(id);
		wiki.views += viewService.increaseWikiViews(id);
		String content = textService.getHtmlFromCache(wiki.textId);
		return prepareModelAndView("wiki.html", Map.of("wiki", wiki, "current", wiki, "content", content));
	}

	@GetMapping("/wiki/" + ID + "/" + ID2)
	public ModelAndView wikiPage(@PathVariable("id") String id, @PathVariable("id2") String pid) {
		Wiki wiki = wikiService.getWikiTreeFromCache(id);
		WikiPage wikiPage = wikiService.getWikiPageById(pid);
		if (!wikiPage.wikiId.equals(id)) {
			return notFound();
		}
		wikiPage.views += viewService.increaseWikiPageViews(pid);
		String content = textService.getHtmlFromCache(wikiPage.textId);
		return prepareModelAndView("wiki.html", Map.of("wiki", wiki, "current", wikiPage, "content", content));
	}

	@GetMapping("/discuss")
	public ModelAndView discuss() {
		List<Board> boards = boardService.getBoards();
		return prepareModelAndView("discuss.html", Map.of("boards", boards));
	}

	@GetMapping("/discuss/" + ID)
	public ModelAndView board(@PathVariable("id") String id,
			@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		Board board = boardService.getBoardFromCache(id);
		PagedResults<Topic> pr = boardService.getTopics(board, pageIndex);
		return prepareModelAndView("board.html", Map.of("board", board, "page", pr.page, "topics", pr.results));
	}

	@GetMapping("/discuss/" + ID + "/" + ID2)
	public ModelAndView topic(@PathVariable("id") String id, @PathVariable("id2") String tid,
			@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		Board board = boardService.getBoardFromCache(id);
		Topic topic = boardService.getTopicById(tid);
		if (!topic.boardId.equals(id)) {
			return notFound();
		}
		PagedResults<Reply> pr = boardService.getReplies(topic, pageIndex);
		// re-construct page:
		int totalItems = pr.page.totalItems + 1;
		int totalPages = totalItems / pr.page.itemsPerPage + (totalItems % pr.page.itemsPerPage > 0 ? 1 : 0);
		Page page = new Page(pr.page.pageIndex, pr.page.itemsPerPage, totalPages, totalItems);
		return prepareModelAndView("topic.html",
				Map.of("board", board, "topic", topic, "page", page, "replies", pr.results));
	}

	@GetMapping("/single/" + ID)
	public ModelAndView singlePage(@PathVariable("id") String id) {
		SinglePage singlePage = singlePageService.getPublishedById(id);
		String content = textService.getHtmlFromCache(singlePage.textId);
		return prepareModelAndView("single.html", Map.of("singlePage", singlePage, "content", content));
	}

	@GetMapping("/user/" + ID)
	public ModelAndView user(@PathVariable("id") String id) {
		User user = userService.getById(id);
		return prepareModelAndView("profile.html", Map.of("user", user));
	}

	@GetMapping("/profile")
	public ModelAndView profile() {
		User user = HttpContext.getRequiredCurrentUser();
		return prepareModelAndView("profile.html", Map.of("user", user));
	}

}
