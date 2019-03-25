package com.itranswarp.web.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itranswarp.bean.ArticleBean;
import com.itranswarp.bean.AttachmentBean;
import com.itranswarp.bean.CategoryBean;
import com.itranswarp.bean.NavigationBean;
import com.itranswarp.bean.SortBean;
import com.itranswarp.enums.Role;
import com.itranswarp.model.Article;
import com.itranswarp.model.Attachment;
import com.itranswarp.model.Category;
import com.itranswarp.model.Navigation;
import com.itranswarp.warpdb.PagedResults;
import com.itranswarp.web.filter.HttpContext;
import com.itranswarp.web.support.RoleWith;

@RestController
public class ApiController extends AbstractController {

	// category ///////////////////////////////////////////////////////////////

	@GetMapping("/api/categories")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<Category>> categories() {
		return Map.of("result", articleService.getCategories());
	}

	@PostMapping("/api/categories/sort")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> categoriesSort(@RequestBody SortBean bean) {
		articleService.sortCategories(bean.ids);
		articleService.removeCategoriesFromCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/api/categories")
	@RoleWith(Role.ADMIN)
	public Category categoryCreate(@RequestBody CategoryBean bean) {
		return articleService.createCategory(bean);
	}

	@PostMapping("/api/categories/" + ID)
	@RoleWith(Role.ADMIN)
	public Category categoryUpdate(@PathVariable("id") long id, @RequestBody CategoryBean bean) {
		Category category = articleService.updateCategory(id, bean);
		articleService.removeCategoryFromCache(id);
		return category;
	}

	@PostMapping("/api/categories/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> categoryDelete(@PathVariable("id") long id) {
		articleService.deleteCategory(id);
		articleService.removeCategoryFromCache(id);
		return API_RESULT_TRUE;
	}

	// article ////////////////////////////////////////////////////////////////

	@GetMapping("/api/articles/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public Article article(@PathVariable("id") long id) {
		return articleService.getById(id);
	}

	@PostMapping("/api/articles")
	@RoleWith(Role.CONTRIBUTOR)
	public Article articleUpdate(@RequestBody ArticleBean bean) {
		return articleService.createArticle(HttpContext.getRequiredCurrentUser(), bean);
	}

	@PostMapping("/api/articles/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public Article articleUpdate(@PathVariable("id") long id, @RequestBody ArticleBean bean) {
		return articleService.updateArticle(HttpContext.getRequiredCurrentUser(), id, bean);
	}

	@PostMapping("/api/articles/" + ID + "/delete")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, Boolean> articleDelete(@PathVariable("id") long id) {
		articleService.deleteArticle(HttpContext.getRequiredCurrentUser(), id);
		return API_RESULT_TRUE;
	}

	// attachment /////////////////////////////////////////////////////////////

	@GetMapping("/api/attachments")
	@RoleWith(Role.CONTRIBUTOR)
	public PagedResults<Attachment> attachments(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
		return attachmentService.getAttachments(pageIndex);
	}

	@GetMapping("/api/attachments/" + ID)
	@RoleWith(Role.CONTRIBUTOR)
	public Attachment attachment(@PathVariable("id") long id) {
		return attachmentService.getById(id);
	}

	@PostMapping("/api/attachments")
	@RoleWith(Role.CONTRIBUTOR)
	public Attachment attachmentCreate(@RequestBody AttachmentBean bean) {
		return attachmentService.createAttachment(HttpContext.getRequiredCurrentUser(), bean);
	}

	@PostMapping("/api/attachments/" + ID + "/delete")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, Boolean> attachmentDelete(@PathVariable("id") long id) {
		attachmentService.deleteAttachment(HttpContext.getRequiredCurrentUser(), id);
		return API_RESULT_TRUE;
	}

	// navigation /////////////////////////////////////////////////////////////

	@GetMapping("/api/navigations")
	@RoleWith(Role.CONTRIBUTOR)
	public Map<String, List<Navigation>> navigations() {
		return Map.of("result", navigationService.getNavigations());
	}

	@PostMapping("/api/navigations/sort")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> navigationsSort(@RequestBody SortBean bean) {
		navigationService.sortNavigations(bean.ids);
		navigationService.removeNavigationsFromCache();
		return API_RESULT_TRUE;
	}

	@PostMapping("/api/navigations")
	@RoleWith(Role.ADMIN)
	public Navigation navigationCreate(@RequestBody NavigationBean bean) {
		return navigationService.createNavigation(bean);
	}

	@PostMapping("/api/navigations/" + ID)
	@RoleWith(Role.ADMIN)
	public Navigation navigationUpdate(@PathVariable("id") long id, @RequestBody NavigationBean bean) {
		Navigation navigation = navigationService.updateNavigation(id, bean);
		navigationService.removeNavigationsFromCache();
		return navigation;
	}

	@PostMapping("/api/navigations/" + ID + "/delete")
	@RoleWith(Role.ADMIN)
	public Map<String, Boolean> navigationDelete(@PathVariable("id") long id) {
		navigationService.deleteNavigation(id);
		navigationService.removeNavigationsFromCache();
		return API_RESULT_TRUE;
	}

}
