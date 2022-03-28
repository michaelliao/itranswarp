package com.itranswarp.service;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.model.Article;
import com.itranswarp.model.Wiki;
import com.itranswarp.model.WikiPage;
import com.itranswarp.warpdb.WarpDb;

@Component
public class ViewDbService {

    @Autowired
    WarpDb db;

    String sqlUpdateArticleView;
    String sqlUpdateWikiView;
    String sqlUpdateWikiPageView;

    @PostConstruct
    public void init() {
        this.sqlUpdateArticleView = "UPDATE " + db.getTable(Article.class) + " SET views = views + ? WHERE id = ?";
        this.sqlUpdateWikiView = "UPDATE " + db.getTable(Wiki.class) + " SET views = views + ? WHERE id = ?";
        this.sqlUpdateWikiPageView = "UPDATE " + db.getTable(WikiPage.class) + " SET views = views + ? WHERE id = ?";
    }

    public long getArticleViews(Long id) {
        Article entity = db.fetch(Article.class, id);
        return entity == null ? 0 : entity.views;
    }

    public long getWikiViews(Long id) {
        Wiki entity = db.fetch(Wiki.class, id);
        return entity == null ? 0 : entity.views;
    }

    public long getWikiPageViews(Long id) {
        WikiPage entity = db.fetch(WikiPage.class, id);
        return entity == null ? 0 : entity.views;
    }

    @Transactional
    public void updateArticleViews(Long id, Long views) {
        this.db.updateSql(this.sqlUpdateArticleView, views, id);
    }

    @Transactional
    public void updateWikiViews(Long id, Long views) {
        this.db.updateSql(this.sqlUpdateWikiView, views, id);
    }

    @Transactional
    public void updateWikiPageViews(Long id, Long views) {
        this.db.updateSql(this.sqlUpdateWikiPageView, views, id);
    }

}
