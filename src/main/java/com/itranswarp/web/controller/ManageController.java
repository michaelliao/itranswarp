package com.itranswarp.web.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;

import com.itranswarp.Application;
import com.itranswarp.bean.setting.AbstractSettingBean;
import com.itranswarp.bean.setting.SettingDefinition;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.Role;
import com.itranswarp.markdown.Markdown;
import com.itranswarp.model.User;
import com.itranswarp.redis.RedisService;
import com.itranswarp.search.AbstractSearcher;
import com.itranswarp.web.filter.HttpContext;
import com.itranswarp.web.view.i18n.Translators;

@Controller
@RequestMapping("/manage")
public class ManageController extends AbstractController {

	@Value("#{applicationConfiguration.name}")
	String name;

	@Value("#{applicationConfiguration.profiles eq 'native'}")
	Boolean dev;

	@Autowired
	Translators translators;

	@Autowired
	LocaleResolver localeResolver;

	@Autowired
	Markdown markdown;

	@Autowired
	RedisService redisService;

	@Autowired(required = false)
	AbstractSearcher searcher;

	@GetMapping("/")
	public ModelAndView index() {
		return prepareModelAndView("redirect:/manage/board/");
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// ad
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/ad/")
	public ModelAndView adSlotList() {
		return prepareModelAndView("manage/ad/adslot_list.html");
	}

	@GetMapping("/ad/adslot_create")
	public ModelAndView adSlotCreate() {
		return prepareModelAndView("manage/ad/adslot_form.html", Map.of("id", 0, "action", "/api/adSlots"));
	}

	@GetMapping("/ad/adslot_update")
	public ModelAndView adSlotUpdate(@RequestParam("id") long id) {
		return prepareModelAndView("manage/ad/adslot_form.html", Map.of("id", id, "action", "/api/adSlots/" + id));
	}

	@GetMapping("/ad/adperiod_list")
	public ModelAndView adPeriodList() {
		return prepareModelAndView("manage/ad/adperiod_list.html", Map.of("today", LocalDate.now().toString()));
	}

	@GetMapping("/ad/adperiod_create")
	public ModelAndView adPeriodCreate() {
		List<User> sponsors = this.userService.getUsersByRole(Role.SPONSOR, 100);
		return prepareModelAndView("manage/ad/adperiod_form.html",
				Map.of("today", LocalDate.now().toString(), "sponsors", sponsors));
	}

	@GetMapping("/ad/admaterial_list")
	public ModelAndView adMaterialList(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return prepareModelAndView("manage/ad/admaterial_list.html", Map.of("page", pageIndex));
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// article and categories
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/article/")
	public ModelAndView articleList(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return prepareModelAndView("manage/article/article_list.html", Map.of("page", pageIndex));
	}

	@GetMapping("/article/category_list")
	public ModelAndView articleCategoryList() {
		var categories = articleService.getCategories();
		return prepareModelAndView("manage/article/category_list.html", Map.of("categories", categories));
	}

	@GetMapping("/article/category_create")
	public ModelAndView articleCategoryCreate() {
		return prepareModelAndView("manage/article/category_form.html", Map.of("id", 0, "action", "/api/categories"));
	}

	@GetMapping("/article/category_update")
	public ModelAndView articleCategoryUpdate(@RequestParam("id") long id) {
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
	// attachment
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/attachment/")
	public ModelAndView attachmentList(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return prepareModelAndView("manage/attachment/attachment_list.html", Map.of("page", pageIndex));
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
	public ModelAndView boardTopicList(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return prepareModelAndView("manage/board/topic_list.html", Map.of("page", pageIndex));
	}

	@GetMapping("/board/reply")
	public ModelAndView boardReplyList(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return prepareModelAndView("manage/board/reply_list.html", Map.of("page", pageIndex));
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// links
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/link/")
	public ModelAndView linkList() {
		return prepareModelAndView("manage/link/link_list.html");
	}

	@GetMapping("/link/link_create")
	public ModelAndView linkCreate() {
		return prepareModelAndView("manage/link/link_form.html", Map.of("id", 0, "action", "/api/links"));
	}

	@GetMapping("/link/link_update")
	public ModelAndView linkUpdate(@RequestParam("id") long id) {
		return prepareModelAndView("manage/link/link_form.html", Map.of("id", id, "action", "/api/links/" + id));
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
	// settings
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/setting/")
	public String setting() {
		return "redirect:/manage/setting/website";
	}

	@GetMapping("/setting/{group}")
	public ModelAndView setting(@PathVariable("group") String group) {
		final List<String> tabs = List.of("website", "snippet", "follow", "security");
		AbstractSettingBean settings = null;
		String tab = null;
		switch (group) {
		case "website":
			tab = "website";
			settings = this.settingService.getWebsite();
			break;
		case "snippet":
			tab = "snippet";
			settings = this.settingService.getSnippet();
			break;
		case "follow":
			tab = "follow";
			settings = this.settingService.getFollow();
			break;
		case "security":
			tab = "security";
			settings = this.settingService.getSecurity();
			break;
		default:
			throw new ApiException(ApiError.PARAMETER_INVALID, "group", "Invalid group name: " + group);
		}
		List<SettingDefinition> definitions = settings.getSettingDefinitions();
		return prepareModelAndView("manage/setting/setting_form.html",
				Map.of("settings", settings, "definitions", definitions, "tab", tab, "tabs", tabs));
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
	// wikis
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/wiki/")
	public ModelAndView wikiList() {
		return prepareModelAndView("manage/wiki/wiki_list.html");
	}

	@GetMapping("/wiki/wiki_create")
	public ModelAndView wikiCreate() {
		return prepareModelAndView("manage/wiki/wiki_form.html", Map.of("id", 0, "action", "/api/wikis"));
	}

	@GetMapping("/wiki/wiki_tree")
	public ModelAndView wikiTree(@RequestParam("id") long id) {
		return prepareModelAndView("manage/wiki/wiki_tree.html", Map.of("id", id));
	}

	@GetMapping("/wiki/wiki_update")
	public ModelAndView wikiUpdate(@RequestParam("id") long id) {
		return prepareModelAndView("manage/wiki/wiki_form.html", Map.of("id", id, "action", "/api/wikis/" + id));
	}

	@GetMapping("/wiki/wikipage_update")
	public ModelAndView wikipageUpdate(@RequestParam("id") long id) {
		return prepareModelAndView("manage/wiki/wikipage_form.html",
				Map.of("id", id, "action", "/api/wikiPages/" + id));
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// search
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/search/")
	public ModelAndView searchStatus() {
		return prepareModelAndView("manage/search/search_status.html", Map.of("searchEnabled", searcher != null,
				"searchEngineName", searcher == null ? "None" : searcher.getEngineName()));
	}

	@PostMapping("/search/reindex")
	public ModelAndView searchReindex() {
		return prepareModelAndView("manage/search/search_reindex.html");
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// users
	///////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/user/")
	public ModelAndView userList(@RequestParam(value = "page", defaultValue = "1") int pageIndex,
			@RequestParam(value = "q", defaultValue = "") String q) {
		return prepareModelAndView("manage/user/user_list.html", Map.of("page", pageIndex, "q", q));
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
