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
import com.itranswarp.enums.Role;
import com.itranswarp.model.Article;
import com.itranswarp.model.Category;
import com.itranswarp.model.User;
import com.itranswarp.util.IdUtil;
import com.itranswarp.warpdb.PagedResults;
import com.itranswarp.web.filter.HttpContext;

@Component
public class ArticleService extends AbstractDbService<Article> {

    @Autowired
    TextService textService;

    @Autowired
    AttachmentService attachmentService;

    @Autowired
    ViewService viewService;

    static final String KEY_RECENT_ARTICLES = "__recent_articles__";
    static final String KEY_ARTICLES_FIRST_PAGE = "__articles__";

    static final String KEY_CATEGORIES = "__categories__";

    static final long KEY_TIMEOUT = 3600;

    public Category getCategoryFromCache(long id) {
        Category c = this.redisService.hget(KEY_CATEGORIES, id, Category.class);
        if (c == null) {
            c = getCategoryById(id);
            this.redisService.hset(KEY_CATEGORIES, id, c);
        }
        return c;
    }

    public void deleteCategoriesFromCache() {
        this.redisService.del(KEY_CATEGORIES);
    }

    public void deleteArticlesFromCache(long categoryId) {
        this.redisService.del(KEY_ARTICLES_FIRST_PAGE + categoryId);
        this.redisService.del(KEY_RECENT_ARTICLES);
    }

    public void deleteCategoryFromCache(long id) {
        this.redisService.hdel(KEY_CATEGORIES, id);
    }

    public List<Category> getCategories() {
        return this.db.from(Category.class).orderBy("displayOrder").list();
    }

    public Category getCategoryById(long id) {
        Category cat = db.fetch(Category.class, id);
        if (cat == null) {
            throw new ApiException(ApiError.ENTITY_NOT_FOUND, "Category", "Category not found.");
        }
        return cat;
    }

    @Transactional
    public Category createCategory(CategoryBean bean) {
        bean.validate(true);
        long maxDisplayOrder = getCategories().stream().mapToLong(c -> c.displayOrder).max().orElseGet(() -> 0);
        Category category = new Category();
        category.name = bean.name;
        category.tag = bean.tag;
        category.description = bean.description;
        category.displayOrder = maxDisplayOrder + 1;
        this.db.insert(category);
        return category;
    }

    @Transactional
    public Category updateCategory(long id, CategoryBean bean) {
        bean.validate(false);
        Category category = this.getCategoryById(id);
        category.name = bean.name;
        category.tag = bean.tag;
        category.description = bean.description;
        this.db.update(category);
        return category;
    }

    @Transactional
    public void deleteCategory(long id) {
        Category category = this.getCategoryById(id);
        if (getArticles(category, 1).page.isEmpty) {
            this.db.remove(category);
        } else {
            throw new ApiException(ApiError.OPERATION_FAILED, "category", "Cannot delete non-empty category.");
        }
    }

    @Transactional
    public void sortCategories(List<Long> ids) {
        List<Category> categories = getCategories();
        sortEntities(categories, ids);
    }

    public List<Article> getPublishedArticles(int maxResults) {
        List<Article> articles = this.redisService.get(KEY_RECENT_ARTICLES, TYPE_LIST_ARTICLE);
        if (articles == null) {
            articles = db.from(Article.class).where("publishAt < ?", System.currentTimeMillis()).orderBy("publishAt").desc().orderBy("id").desc()
                    .limit(maxResults).list();
            this.redisService.set(KEY_RECENT_ARTICLES, articles, KEY_TIMEOUT);
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
            articles = this.db.from(Article.class).where("categoryId = ? AND publishAt < ?", category.id, ts).orderBy("publishAt").desc().orderBy("id").desc()
                    .list(pageIndex, ITEMS_PER_PAGE);
            if (pageIndex == 1) {
                this.redisService.set(KEY_ARTICLES_FIRST_PAGE + category.id, articles, KEY_TIMEOUT);
            }
        }
        return articles;
    }

    public PagedResults<Article> getArticles(int pageIndex) {
        return this.db.from(Article.class).orderBy("publishAt").desc().orderBy("id").desc().list(pageIndex, ITEMS_PER_PAGE);
    }

    public PagedResults<Article> getArticles(int pageIndex, int pageSize) {
        return this.db.from(Article.class).orderBy("publishAt").desc().orderBy("id").desc().list(pageIndex, pageSize);
    }

    public PagedResults<Article> getArticles(Category category, int pageIndex) {
        return this.db.from(Article.class).where("categoryId = ?", category.id).orderBy("publishAt").desc().orderBy("id").desc().list(pageIndex,
                ITEMS_PER_PAGE);
    }

    public Article getPublishedById(long id) {
        Article article = getById(id);
        if (article.publishAt > System.currentTimeMillis()) {
            User user = HttpContext.getCurrentUser();
            if (user == null || user.role.value > Role.CONTRIBUTOR.value) {
                throw new ApiException(ApiError.ENTITY_NOT_FOUND, "Article", "Article not found.");
            }
        }
        return article;
    }

    @Transactional
    public Article createArticle(User user, ArticleBean bean) {
        bean.validate(true);
        getCategoryById(bean.categoryId);
        Article article = new Article();
        article.id = IdUtil.nextId();
        article.userId = user.id;
        article.categoryId = bean.categoryId;
        article.name = bean.name;
        article.description = bean.description;
        article.publishAt = bean.publishAt;
        article.tags = bean.tags;

        AttachmentBean atta = new AttachmentBean();
        atta.name = article.name;
        atta.data = bean.image;
        article.imageId = attachmentService.createAttachment(user, atta).id;

        article.textId = textService.createText(bean.content).id;

        this.db.insert(article);
        return article;
    }

    @Transactional
    public Article deleteArticle(User user, long id) {
        Article article = this.getById(id);
        checkPermission(user, article.userId);
        this.db.remove(article);
        return article;
    }

    @Transactional
    public Article updateArticle(User user, long id, ArticleBean bean) {
        bean.validate(false);
        Article article = this.getById(id);
        checkPermission(user, article.userId);
        article.categoryId = bean.categoryId;
        article.name = bean.name;
        article.description = bean.description;
        article.publishAt = bean.publishAt;
        article.tags = bean.tags;
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
