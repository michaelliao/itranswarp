package com.itranswarp.search;

import java.io.IOException;
import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.itranswarp.common.AbstractService;
import com.itranswarp.markdown.Markdown;
import com.itranswarp.model.Article;
import com.itranswarp.model.SinglePage;
import com.itranswarp.model.Wiki;
import com.itranswarp.model.WikiPage;
import com.itranswarp.service.TextService;
import com.itranswarp.warpdb.Page;
import com.itranswarp.warpdb.PagedResults;

public abstract class AbstractSearcher extends AbstractService {

    @Value("${spring.search.highlight-style-name:x-highlight}")
    private String highlightStyle = "x-highlight";

    @Value("${spring.search.max-query-length:50}")
    private int maxQueryLength;

    @Value("${spring.search.max-keywords:3}")
    private int maxKeywords;

    @Value("${spring.search.fragment-size:30}")
    private int fragmentSize;

    @Value("${spring.search.max-page-index:5}")
    private int maxPageIndex;

    @Value("${spring.search.page-size:10}")
    private int pageSize;

    private String highlightPreTag = "<span class=\"x-highlight\">";
    private String highlightPostTag = "</span>";

    @Autowired
    private Markdown markdown;

    @Autowired
    private TextService textService;

    @PostConstruct
    public void initAbstractSearcher() {
        this.highlightPreTag = "<span class=\"" + this.highlightStyle + "\">";
    }

    /**
     * Get engine name.
     * 
     * @return Engine name.
     */
    public abstract String getEngineName();

    protected int getFragmentSize() {
        return this.fragmentSize;
    }

    protected String getHighlightPreTag() {
        return this.highlightPreTag;
    }

    protected String getHighlightPostTag() {
        return this.highlightPostTag;
    }

    /**
     * Parse query to String array, or null if parse failed.
     * 
     * @param q
     * @return
     */
    private String[] parseQuery(String q) {
        if (q == null) {
            return null;
        }
        if (q.length() > this.maxQueryLength) {
            q = q.substring(0, this.maxQueryLength);
        }
        q = q.strip();
        if (q.isEmpty()) {
            return null;
        }
        String[] ss = q.split("[\\s\\;\\,\\.\\?\\+\\&\\-\\？\\，\\、\\。\\；]+");
        String[] qs = Arrays.stream(ss).filter(s -> !s.isEmpty()).limit(this.maxKeywords).toArray(String[]::new);
        if (qs.length == 0) {
            return null;
        }
        return qs;
    }

    /**
     * Search by keywords and return results. Return null if search condition is
     * invalid.
     */
    public PagedResults<SearchableDocument> search(String q, int pageIndex) throws Exception {
        String[] qs = parseQuery(q);
        if (qs == null) {
            return null;
        }
        if (pageIndex < 1) {
            pageIndex = 1;
        } else if (pageIndex > this.maxPageIndex) {
            pageIndex = this.maxPageIndex;
        }
        Hits hits = search(qs, System.currentTimeMillis(), pageIndex * this.pageSize, (pageIndex - 1) * this.pageSize, this.pageSize);
        logger.info("hits: total = {}, page = {}, returned = {}.", hits.total, pageIndex, hits.documents.size());
        int totalItems = hits.total;
        int totalPages = totalItems / this.pageSize + (totalItems % this.pageSize > 0 ? 1 : 0);
        if (totalPages > this.maxPageIndex) {
            totalPages = this.maxPageIndex;
        }
        Page page = new Page(pageIndex, this.pageSize, totalPages, totalItems);
        return new PagedResults<>(page, hits.documents);
    }

    /**
     * Search by parsed keywords.
     * 
     * @param qs         Parsed keywords.
     * @param maxResults The max result list.
     * @param offset     The offset of result list.
     * @param size       The size of result list.
     * @return Hits object.
     * @throws IOException
     */
    protected abstract Hits search(String[] qs, long timestamp, int maxResults, int offset, int size) throws Exception;

    private SearchableDocument toSearchableDocument(Article obj) {
        SearchableDocument document = new SearchableDocument();
        document.type = "article";
        document.id = obj.id;
        document.name = obj.name;
        document.publishAt = obj.publishAt;
        document.content = markdown.toText(textService.getById(obj.textId).content);
        document.url = "/article/" + obj.id;
        return document;
    }

    private SearchableDocument toSearchableDocument(Wiki obj) {
        SearchableDocument document = new SearchableDocument();
        document.type = "wiki";
        document.id = obj.id;
        document.name = obj.name;
        document.publishAt = obj.publishAt;
        document.content = markdown.toText(textService.getById(obj.textId).content);
        document.url = "/wiki/" + obj.id;
        return document;
    }

    private SearchableDocument toSearchableDocument(WikiPage obj) {
        SearchableDocument document = new SearchableDocument();
        document.type = "wikipage";
        document.id = obj.id;
        document.name = obj.name;
        document.publishAt = obj.publishAt;
        document.content = markdown.toText(textService.getById(obj.textId).content);
        document.url = "/wiki/" + obj.parentId + "/" + obj.id;
        return document;
    }

    private SearchableDocument toSearchableDocument(SinglePage obj) {
        SearchableDocument document = new SearchableDocument();
        document.type = "singlepage";
        document.id = obj.id;
        document.name = obj.name;
        document.publishAt = obj.publishAt;
        document.content = markdown.toText(textService.getById(obj.textId).content);
        document.url = "/single/" + obj.id;
        return document;
    }

    public void indexArticles(Article... objs) throws Exception {
        var documents = Arrays.stream(objs).map(this::toSearchableDocument).toArray(SearchableDocument[]::new);
        indexSearchableDocuments(documents);
    }

    public void indexWiki(Wiki obj) throws Exception {
        indexSearchableDocuments(this.toSearchableDocument(obj));
    }

    public void indexWikiPages(WikiPage... objs) throws Exception {
        var documents = Arrays.stream(objs).map(this::toSearchableDocument).toArray(SearchableDocument[]::new);
        indexSearchableDocuments(documents);
    }

    public void indexSinglePages(SinglePage... objs) throws Exception {
        var documents = Arrays.stream(objs).map(this::toSearchableDocument).toArray(SearchableDocument[]::new);
        indexSearchableDocuments(documents);
    }

    /**
     * DANGER operation: Remove ALL index.
     * 
     * @throws Exception
     */
    public abstract void removeAllIndex() throws Exception;

    /**
     * Add searchable documents.
     */
    protected abstract void indexSearchableDocuments(SearchableDocument... documents) throws Exception;

    /**
     * Remove a searchable document.
     * 
     * @param id
     * @throws IOException
     */
    public abstract void unindexSearchableDocument(long id) throws Exception;

}
