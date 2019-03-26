package com.itranswarp.web.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;

import com.itranswarp.Application;
import com.itranswarp.markdown.Markdown;
import com.itranswarp.web.filter.HttpContext;
import com.itranswarp.web.view.i18n.Translators;

@Controller
@RequestMapping("/manage")
public class ManageController extends AbstractController {

	@Value("#{applicationConfiguration.name}")
	String name;

	@Value("#{applicationConfiguration.cdn}")
	String cdn;

	@Value("#{applicationConfiguration.profiles eq 'native'}")
	Boolean dev;

	@Autowired
	Translators translators;

	@Autowired
	LocaleResolver localeResolver;

	@Autowired
	Markdown markdown;

	@GetMapping("/")
	public ModelAndView index() {
		return prepareModelAndView("redirect:/manage/board/");
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// article and categories
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/article/")
	public ModelAndView articleList(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return prepareModelAndView("manage/article/article_list.html", Map.of("page", pageIndex));
	}

	@GetMapping("/article/category_list")
	public ModelAndView categoryList() {
		var categories = articleService.getCategories();
		return prepareModelAndView("manage/article/category_list.html", Map.of("categories", categories));
	}

	@GetMapping("/article/category_create")
	public ModelAndView categoryCreate() {
		return prepareModelAndView("manage/article/category_form.html", Map.of("id", 0, "action", "/api/categories"));
	}

	@GetMapping("/article/category_update")
	public ModelAndView categoryUpdate(@RequestParam("id") long id) {
		return prepareModelAndView("manage/article/category_form.html",
				Map.of("id", id, "action", "/api/categories/" + id));
	}

	@GetMapping("/article/article_create")
	public ModelAndView articleCreate() {
		return prepareModelAndView("manage/article/article_form.html", Map.of("id", 0, "action", "/api/articles"));
	}

	@GetMapping("/article/article_update")
	public ModelAndView articleUpdate(@RequestParam("id") long id) {
		return prepareModelAndView("manage/article/article_form.html",
				Map.of("id", id, "action", "/api/articles/" + id));
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// single pages
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/single/")
	public ModelAndView singlePageList() {
		var singlePages = this.singlePageService.getAll();
		return prepareModelAndView("manage/single/singlepage_list.html", Map.of("singlePages", singlePages));
	}

	@GetMapping("/single/create")
	public ModelAndView singlePageCreate() {
		return prepareModelAndView("manage/single/singlepage_form.html", Map.of("id", 0, "action", "/api/singlePages"));
	}

	@GetMapping("/single/update")
	public ModelAndView singlePageUpdate(@RequestParam("id") long id) {
		return prepareModelAndView("manage/single/singlepage_form.html",
				Map.of("id", id, "action", "/api/singlePages/" + id));
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// boards, topics and replies
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/board/")
	public ModelAndView boardList() {
		return prepareModelAndView("manage/board/board_list.html");
	}

	@GetMapping("/board/board_create")
	public ModelAndView boardCreate() {
		return prepareModelAndView("manage/board/board_form.html", Map.of("id", 0, "action", "/api/boards"));
	}

	@GetMapping("/board/board_update")
	public ModelAndView boardUpdate(@RequestParam("id") long id) {
		return prepareModelAndView("manage/board/board_form.html", Map.of("id", id, "action", "/api/boards/" + id));
	}

	@GetMapping("/board/topic")
	public ModelAndView topicList(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return prepareModelAndView("manage/board/topic_list.html", Map.of("page", pageIndex));
	}

	@GetMapping("/board/reply")
	public ModelAndView replyList(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return prepareModelAndView("manage/board/reply_list.html", Map.of("page", pageIndex));
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// navigations
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/navigation/")
	public ModelAndView navigationList() {
		return prepareModelAndView("manage/navigation/navigation_list.html");
	}

	@GetMapping("/navigation/navigation_create")
	public ModelAndView navigationCreate() {
		return prepareModelAndView("manage/navigation/navigation_form.html",
				Map.of("id", 0, "action", "/api/navigations"));
	}

	@GetMapping("/navigation/navigation_update")
	public ModelAndView navigationUpdate(@RequestParam("id") long id) {
		return prepareModelAndView("manage/navigation/navigation_form.html",
				Map.of("id", id, "action", "/api/navigations/" + id));
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// utility
	///////////////////////////////////////////////////////////////////////////////////////////////

	private ModelAndView prepareModelAndView(String view) {
		ModelAndView mv = new ModelAndView(view);
		appendGlobalModelAndView(mv);
		return mv;
	}

	private ModelAndView prepareModelAndView(String view, Map<String, Object> model) {
		ModelAndView mv = new ModelAndView(view, model);
		appendGlobalModelAndView(mv);
		return mv;
	}

	private void appendGlobalModelAndView(ModelAndView mv) {
		@SuppressWarnings("resource")
		HttpContext ctx = HttpContext.getContext();
		mv.addObject("__name__", this.name);
		mv.addObject("__cdn__", this.cdn);
		mv.addObject("__dev__", this.dev);
		mv.addObject("__website__", settingService.getWebsiteFromCache());
		mv.addObject("__user__", ctx.user);
		mv.addObject("__navigations__", navigationService.getNavigationsFromCache());
		mv.addObject("__timestamp__", ctx.timestamp);
		mv.addObject("__translator__", translators.getTranslator(localeResolver.resolveLocale(ctx.request)));
		mv.addObject("__version__", Application.VERSION);
		ctx.response.setHeader("X-Execution-Time", String.valueOf(System.currentTimeMillis() - ctx.timestamp));
	}
}
