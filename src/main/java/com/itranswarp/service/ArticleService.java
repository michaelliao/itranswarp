package com.itranswarp.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.itranswarp.bean.ArticleBean;
import com.itranswarp.bean.AttachmentBean;
import com.itranswarp.bean.CategoryBean;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.model.Article;
import com.itranswarp.model.Category;
import com.itranswarp.model.User;
import com.itranswarp.util.IdUtil;
import com.itranswarp.warpdb.PagedResults;

@Component
public class ArticleService extends AbstractService<Article> {

	@Autowired
	TextService textService;

	@Autowired
	AttachmentService attachmentService;

	@Autowired
	ViewService viewService;

	static final String KEY_RECENT_ARTICLES = "_recent_articles_";
	static final String KEY_ARTICLES_FIRST_PAGE = "_articles_";
	static final long CACHE_ARTICLES_SECONDS = 3600;

	static final String KEY_CATEGORIES = "_categories";

	public Category getCategoryFromCache(String id) {
		Category c = this.redisService.hget(KEY_CATEGORIES, id, Category.class);
		if (c == null) {
			c = getCategoryById(id);
			this.redisService.hset(KEY_CATEGORIES, id, c);
		}
		return c;
	}

	public void removeCategoriesFromCache() {
		this.redisService.del(KEY_CATEGORIES);
	}

	public void removeCategoryFromCache(String id) {
		this.redisService.hdel(KEY_CATEGORIES, id);
	}

	public List<Category> getCategories() {
		return this.db.from(Category.class).orderBy("displayOrder").list();
	}

	public Category getCategoryById(String id) {
		Category cat = db.fetch(Category.class, id);
		if (cat == null) {
			throw new ApiException(ApiError.ENTITY_NOT_FOUND, "Category", "Category not found.");
		}
		return cat;
	}

	@Transactional
	public Category createCategory(CategoryBean bean) {
		long maxDisplayOrder = getCategories().stream().mapToLong(c -> c.displayOrder).max().orElseGet(() -> 0);
		Category category = new Category();
		category.name = checkName(bean.name);
		category.description = checkDescription(bean.description);
		category.displayOrder = maxDisplayOrder + 1;
		this.db.insert(category);
		return category;
	}

	@Transactional
	public Category updateCategory(String id, CategoryBean bean) {
		Category category = this.getCategoryById(id);
		if (bean.name != null) {
			category.name = checkName(category.name);
		}
		if (bean.description != null) {
			category.description = checkDescription(category.description);
		}
		this.db.update(category);
		return category;
	}

	@Transactional
	public void deleteCategory(String id) {
		Category category = this.getCategoryById(id);
		if (getArticles(category, 1).page.isEmpty) {
			this.db.remove(category);
		} else {
			throw new ApiException(ApiError.OPERATION_FAILED, "category", "Cannot delete non-empty category.");
		}
	}

	@Transactional
	public void sortCategories(List<String> ids) {
		List<Category> categories = getCategories();
		sortEntities(categories, ids);
	}

	public List<Article> getPublishedArticles(int maxResults) {
		List<Article> articles = this.redisService.get(KEY_RECENT_ARTICLES, TYPE_LIST_ARTICLE);
		if (articles == null) {
			articles = db.from(Article.class).where("publishAt < ?", System.currentTimeMillis()).orderBy("publishAt")
					.desc().limit(maxResults).list();
			this.redisService.set(KEY_RECENT_ARTICLES, articles);
		}
		return articles;
	}

	public PagedResults<Article> getPublishedArticles(Category category, int pageIndex) {
		long ts = System.currentTimeMillis();
		PagedResults<Article> articles = null;
		if (pageIndex == 1) {
			articles = this.redisService.get(KEY_ARTICLES_FIRST_PAGE + category.id, TYPE_PAGE_RESULTS_ARTICLE);
		}
		if (articles == null) {
			articles = this.db.from(Article.class).where("categoryId = ? AND publishAt < ?", category.id, ts)
					.orderBy("publishAt").desc().list(pageIndex, ITEMS_PER_PAGE);
			if (pageIndex == 1) {
				this.redisService.set(KEY_ARTICLES_FIRST_PAGE + category.id, articles, CACHE_ARTICLES_SECONDS);
			}
		}
		return articles;
	}

	public PagedResults<Article> getArticles(Category category, int pageIndex) {
		return this.db.from(Article.class).where("categoryId = ?", category.id).list(pageIndex, ITEMS_PER_PAGE);
	}

	public Article getPublishedById(String id) {
		Article article = getById(id);
		if (article.publishAt > System.currentTimeMillis()) {
			throw new ApiException(ApiError.ENTITY_NOT_FOUND, "Article", "Article not found.");
		}
		return article;
	}

	@Transactional
	public Article createArticle(User user, ArticleBean bean) {
		getCategoryById(bean.categoryId);
		Article article = new Article();
		article.id = IdUtil.nextId();
		article.categoryId = bean.categoryId;
		article.name = checkName(bean.name);
		article.description = checkDescription(bean.description);
		article.publishAt = checkPublishAt(bean.publishAt);
		article.tags = checkTags(bean.tags);

		AttachmentBean atta = new AttachmentBean();
		atta.name = article.name;
		atta.data = bean.image;
		article.imageId = attachmentService.createAttachment(user, atta).id;

		article.textId = textService.createText(bean.content).id;

		this.db.insert(article);
		return article;
	}

	@Transactional
	public void deleteArticle(User user, String id) {
		Article article = this.getById(id);
		checkPermission(user, article.userId);
		this.db.remove(article);
	}

	@Transactional
	public Article updateArticle(User user, String id, ArticleBean bean) {
		Article article = this.getById(id);
		checkPermission(user, article.userId);
		if (bean.categoryId != null) {
			getCategoryById(bean.categoryId);
			article.categoryId = bean.categoryId;
		}
		if (bean.name != null) {
			article.name = checkName(bean.name);
		}
		if (bean.description != null) {
			article.description = checkDescription(bean.description);
		}
		if (bean.tags != null) {
			article.tags = checkTags(bean.tags);
		}
		if (bean.publishAt != null) {
			article.publishAt = checkPublishAt(bean.publishAt);
		}
		if (bean.image != null) {
			AttachmentBean atta = new AttachmentBean();
			atta.name = article.name;
			atta.data = bean.image;
			article.imageId = attachmentService.createAttachment(user, atta).id;
		}
		if (bean.content != null) {
			article.textId = textService.createText(bean.content).id;
		}
		this.db.update(article);
		return article;
	}

	static final TypeReference<List<Article>> TYPE_LIST_ARTICLE = new TypeReference<>() {
	};

	static final TypeReference<PagedResults<Article>> TYPE_PAGE_RESULTS_ARTICLE = new TypeReference<>() {
	};
}
