package com.itranswarp.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.itranswarp.bean.HeadlineBean;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.model.Headline;
import com.itranswarp.warpdb.PagedResults;

@Component
public class HeadlineService extends AbstractDbService<Headline> {

    static final int MAX_PUBLISHED_PAGES = 100;

    static final String KEY_HEADLINE = "__hl__";

    static final long KEY_TIMEOUT = 3600;

    public void deleteHeadlinesFromCache() {
        this.redisService.del(KEY_HEADLINE);
    }

    public List<Headline> getPublishedHeadlinesFromCache() {
        List<Headline> headlines = this.redisService.get(KEY_HEADLINE, TYPE_LIST_HEADLINE);
        if (headlines == null) {
            headlines = this.db.from(Headline.class).where("published = ?", Boolean.TRUE).orderBy("publishedAt").desc().limit(super.ITEMS_PER_PAGE).list();
            this.redisService.set(KEY_HEADLINE, headlines, KEY_TIMEOUT);
        }
        return headlines;
    }

    public PagedResults<Headline> getPublishedHeadlines(int pageIndex) {
        return this.db.from(Headline.class).where("published = ?", Boolean.TRUE).orderBy("publishAt").desc().list(pageIndex, super.ITEMS_PER_PAGE);
    }

    public PagedResults<Headline> getUnpublishedHeadlines(int pageIndex) {
        return this.db.from(Headline.class).where("published = ?", Boolean.FALSE).orderBy("createdAt").desc().orderBy("id").desc().list(pageIndex,
                super.ITEMS_PER_PAGE);
    }

    @Transactional
    public Headline deleteHeadline(long id) {
        Headline hl = this.getById(id);
        this.db.remove(hl);
        return hl;
    }

    @Transactional
    public void deleteExpiredPublishedHeadlines() {
        PagedResults<Headline> pr = getPublishedHeadlines(MAX_PUBLISHED_PAGES);
        if (pr.results.size() < super.ITEMS_PER_PAGE) {
            return;
        }
        Headline last = pr.results.get(super.ITEMS_PER_PAGE - 1);
        db.updateSql("DELETE FROM " + db.getTable(Headline.class) + " WHERE published = ? AND publishAt < ?", Boolean.TRUE, last.publishAt);
    }

    @Transactional
    public Headline createHeadline(long userId, HeadlineBean bean) {
        bean.validate(true);
        Headline hl = new Headline();
        hl.copyPropertiesFrom(bean);
        hl.userId = userId;
        hl.published = false;
        hl.publishAt = System.currentTimeMillis();
        this.db.insert(hl);
        return hl;
    }

    @Transactional
    public Headline updateHeadline(long id, HeadlineBean bean) {
        bean.validate(false);
        Headline hl = getHeadline(id);
        hl.copyPropertiesFrom(bean);
        this.db.update(hl);
        return hl;
    }

    @Transactional
    public Headline publishHeadline(long id) {
        Headline hl = this.getById(id);
        if (hl.published) {
            return hl;
        }
        hl.published = true;
        hl.publishAt = System.currentTimeMillis();
        this.db.update(hl);
        return hl;
    }

    public Headline getHeadline(long id) {
        Headline hl = this.db.fetch(Headline.class, id);
        if (hl == null) {
            throw new ApiException(ApiError.ENTITY_NOT_FOUND, "Headline", "Headline not found.");
        }
        return hl;
    }

    static final TypeReference<List<Headline>> TYPE_LIST_HEADLINE = new TypeReference<>() {
    };

}
