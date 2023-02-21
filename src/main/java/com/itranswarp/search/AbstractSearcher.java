package com.itranswarp.search;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.annotation.PostConstruct;

import org.lionsoul.jcseg.ISegment;
import org.lionsoul.jcseg.dic.ADictionary;
import org.lionsoul.jcseg.dic.HashMapDictionary;
import org.lionsoul.jcseg.extractor.impl.TextRankKeywordsExtractor;
import org.lionsoul.jcseg.segmenter.SegmenterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.itranswarp.common.AbstractService;
import com.itranswarp.markdown.Markdown;
import com.itranswarp.model.Article;
import com.itranswarp.model.Wiki;
import com.itranswarp.model.WikiPage;
import com.itranswarp.service.ArticleService;
import com.itranswarp.service.TextService;
import com.itranswarp.service.WikiService;
import com.itranswarp.warpdb.PagedResults;

public abstract class AbstractSearcher extends AbstractService {

    public static enum IndexStatus {
        /**
         * Index is newly created.
         */
        CREATED,

        /**
         * Index exist.
         */
        EXIST,

        /**
         * Index is just removed.
         */
        REMOVED,

        /**
         * Index create/remove failed.
         */
        FAILED
    }

    @Value("${spring.search.max-query-length:50}")
    private int maxQueryLength;

    @Autowired
    private Markdown markdown;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private WikiService wikiService;

    @Autowired
    private TextService textService;

    private SegmenterConfig config;
    private ADictionary dict;

    private boolean isReady = false;

    @PostConstruct
    public void init() throws Exception {
        this.config = new SegmenterConfig(true);
        this.config.EN_WORD_SEG = false;
        this.dict = createCustomizedDictionary(config, true, true);
        createIndex();
    }

    /**
     * copied from <code>DictionaryFactory.createDefaultDictionary()</code> to fix
     * loading from classpath:
     */
    static ADictionary createCustomizedDictionary(SegmenterConfig config, boolean sync, boolean loadDic) {
        final ADictionary dic = new HashMapDictionary(config, sync) {
            /**
             * Fix load classpath in SpringBoot:
             */
            @Override
            public void loadClassPath() throws IOException {
                final Class<?> dClass = ADictionary.class;
                final CodeSource codeSrc = dClass.getProtectionDomain().getCodeSource();
                if (codeSrc == null) {
                    return;
                }

                final String codePath = codeSrc.getLocation().getPath();
                // FIXME: add test for endsWith(".jar!/"):
                if (codePath.toLowerCase().endsWith(".jar") || codePath.toLowerCase().endsWith(".jar!/")) {
                    final ZipInputStream zip = new ZipInputStream(codeSrc.getLocation().openStream());
                    while (true) {
                        ZipEntry e = zip.getNextEntry();
                        if (e == null) {
                            break;
                        }

                        String fileName = e.getName();
                        if (fileName.endsWith(".lex") && fileName.startsWith("lexicon/lex-")) {
                            load(dClass.getResourceAsStream("/" + fileName));
                        }
                    }
                } else {
                    // now, the classpath is an IDE directory
                    // like eclipse ./bin or maven ./target/classes/
                    final File lexiconDir = new File(URLDecoder.decode(codeSrc.getLocation().getFile(), "utf-8"));
                    loadDirectory(lexiconDir.getPath() + File.separator + "lexicon");
                }
            }

        };
        if (!loadDic) {
            return dic;
        }

        try {
            /*
             * @Note: updated at 2016/07/07
             * 
             * check and load all the lexicons with more than one path if specified none
             * lexicon paths (config.getLexiconPath() is null) And we directly load the
             * default lexicons that in the class path
             */
            String[] lexPath = config.getLexiconPath();
            if (lexPath == null) {
                dic.loadClassPath();
            } else {
                for (String lPath : lexPath)
                    dic.loadDirectory(lPath);
                if (config.isAutoload())
                    dic.startAutoload();
            }

            /*
             * added at 2017/06/10 check and reset synonyms net of the current Dictionary
             */
            dic.resetSynonymsNet();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dic;
    }

    public boolean ready() {
        return this.isReady;
    }

    public void indexAll() throws Exception {
        logger.info("Try reindex all documents...");
        for (int page = 1; page < 1000; page++) {
            PagedResults<Article> r = articleService.getArticles(page, 1000);
            if (page > r.page.totalPages) {
                break;
            }
            logger.info("try index {} articles...", r.results.size());
            indexSearchableDocuments(r.results.stream().map(this::toSearchableDocument).collect(Collectors.toList()));
        }
        for (Wiki wiki : wikiService.getWikis()) {
            List<WikiPage> wps = wikiService.getWikiPages(wiki.id);

            logger.info("try index 1 wiki...");
            indexSearchableDocuments(List.of(this.toSearchableDocument(wiki)));
            logger.info("try index {} wiki pages...", wps.size());
            indexSearchableDocuments(wps.stream().map(wp -> this.toSearchableDocument(wiki, wp)).collect(Collectors.toList()));
        }
    }

    public SearchableDocument toSearchableDocument(Article a) {
        var doc = new SearchableDocument();
        doc.id = a.id;
        doc.type = "article";
        doc.name = a.name;
        doc.content = a.description + "\n\n" + markdown.toText(textService.getById(a.textId).content);
        doc.publishAt = a.publishAt;
        doc.url = "/article/" + a.id;
        return doc;
    }

    public SearchableDocument toSearchableDocument(Wiki w) {
        var doc = new SearchableDocument();
        doc.id = w.id;
        doc.type = SearchableDocument.TYPE_WIKI;
        doc.name = w.name;
        doc.content = w.description + "\n\n" + markdown.toText(textService.getById(w.textId).content);
        doc.publishAt = w.publishAt;
        doc.url = "/wiki/" + w.id;
        return doc;
    }

    public SearchableDocument toSearchableDocument(Wiki wiki, WikiPage wp) {
        var doc = new SearchableDocument();
        doc.id = wp.id;
        doc.type = SearchableDocument.TYPE_WIKI_PAGE;
        doc.name = wiki.name + " / " + wp.name;
        doc.content = markdown.toText(textService.getById(wp.textId).content);
        doc.publishAt = Long.max(wiki.publishAt, wp.publishAt);
        doc.url = "/wiki/" + wp.wikiId + "/" + wp.id;
        return doc;
    }

    /**
     * Get engine name.
     * 
     * @return Engine name.
     */
    public abstract String getEngineName();

    /**
     * Parse query to String array, or null if parse failed.
     */
    public List<String> parseQuery(String q) throws IOException {
        if (!isReady) {
            return null;
        }
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
        q = q.replaceAll("[\\s\\:\\/\\;\\,\\.\\*\\<\\>\\?\\!\\+\\&\\-\\？\\，\\、\\。\\！\\？\\；]+", " ");

        ISegment segment = ISegment.MOST.factory.create(config, dict);
        TextRankKeywordsExtractor extractor = new TextRankKeywordsExtractor(segment);
        List<String> qs = extractor.getKeywords(new StringReader(q));
        return qs;
    }

    /**
     * Search by keywords and return results. Return null if search condition is
     * invalid.
     */
    public Hits search(List<String> qs, int maxResults) throws Exception {
        if (!isReady) {
            return Hits.empty();
        }
        Hits hits = search(qs, maxResults, System.currentTimeMillis());
        logger.info("hits: total = {}.", hits.total);
        return hits;
    }

    /**
     * Search by parsed keywords.
     * 
     * @param qs         Parsed keywords.
     * @param maxResults The max result list.
     * @param timestamp  Current timestamp.
     * @return Hits object.
     * @throws IOException
     */
    protected abstract Hits search(List<String> qs, int maxResults, long timestamp) throws Exception;

    public void indexArticles(Article... objs) throws Exception {
        if (!isReady) {
            logger.warn("skip index article for engine is not ready.");
            return;
        }
        var documents = Arrays.stream(objs).map(this::toSearchableDocument).collect(Collectors.toList());
        indexSearchableDocuments(documents);
    }

    public void indexWiki(Wiki wiki) throws Exception {
        if (!isReady) {
            logger.warn("skip index wiki for engine is not ready.");
            return;
        }
        indexSearchableDocuments(List.of(this.toSearchableDocument(wiki)));
    }

    public void indexWikiPages(Wiki wiki, List<WikiPage> objs) throws Exception {
        if (!isReady) {
            logger.warn("skip index wikipage for engine is not ready.");
            return;
        }
        var documents = objs.stream().map(wp -> toSearchableDocument(wiki, wp)).collect(Collectors.toList());
        indexSearchableDocuments(documents);
    }

    public void createIndex() throws Exception {
        IndexStatus status = doInitIndex();
        switch (status) {
        case CREATED -> {
            indexAll();
            isReady = true;
        }
        case EXIST -> isReady = true;
        case FAILED -> logger.error("index could not be initialized and search is disabled.");
        case REMOVED -> throw new IllegalArgumentException("Unexpected value: " + status);
        default -> throw new IllegalArgumentException("Unexpected value: " + status);
        }
    }

    /**
     * DANGER operation: Remove ALL index.
     * 
     * @throws Exception
     */
    public void removeIndex() throws Exception {
        if (!isReady) {
            logger.warn("skip remove index for engine is not ready.");
            return;
        }
        IndexStatus status = doRemoveIndex();
        switch (status) {
        case REMOVED -> isReady = false;
        case FAILED -> logger.error("index could not be removed.");
        case CREATED, EXIST -> throw new IllegalArgumentException("Unexpected value: " + status);
        default -> throw new IllegalArgumentException("Unexpected value: " + status);
        }
    }

    protected abstract IndexStatus doInitIndex() throws Exception;

    protected abstract IndexStatus doRemoveIndex() throws Exception;

    /**
     * Add searchable documents.
     */
    protected abstract void indexSearchableDocuments(List<SearchableDocument> documents) throws Exception;

    /**
     * Remove a searchable document.
     * 
     * @param id
     * @throws IOException
     */
    public abstract boolean unindexSearchableDocument(long id) throws Exception;

}
