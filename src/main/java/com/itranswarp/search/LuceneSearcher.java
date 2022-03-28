package com.itranswarp.search;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.search.lucene.enabled", havingValue = "true")
public class LuceneSearcher extends AbstractSearcher {

    @Value("${spring.search.lucene.index-dir:/var/search-index}")
    private String indexDir;

    private Analyzer analyzer;
    private Directory directory;
    private IndexWriter indexWriter;
    private DirectoryReader indexReader;

    @PostConstruct
    public void init() throws IOException {
        logger.info("init lucene search engine...");
        this.analyzer = new StandardAnalyzer();
        this.directory = FSDirectory.open(Paths.get(indexDir));
        this.indexWriter = new IndexWriter(this.directory, new IndexWriterConfig(this.analyzer));
        this.indexReader = DirectoryReader.open(this.indexWriter);
    }

    @PreDestroy
    public void shutdown() throws Exception {
        this.indexReader.close();
        this.indexWriter.commit();
        this.indexWriter.close();
    }

    @Override
    public String getEngineName() {
        return "Lucene";
    }

    @Override
    protected Hits search(String[] qs, long timestamp, int maxResults, int offset, int size) throws Exception {
        DirectoryReader changed = DirectoryReader.openIfChanged(this.indexReader, this.indexWriter);
        if (changed != null) {
            logger.info("re-open index reader since index changed.");
            this.indexReader = changed;
        }
        IndexSearcher searcher = new IndexSearcher(this.indexReader);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(LongPoint.newRangeQuery(SearchableDocument.FIELD_PUBLISH_AT, 0, timestamp), Occur.MUST);
        builder.add(buildMultiQuery(qs), Occur.MUST);
        Query query = builder.build();
        TopDocs docs = searcher.search(query, maxResults);
        if (offset >= docs.scoreDocs.length) {
            return new Hits(0, offset, List.of());
        }
        Formatter formatter = new SimpleHTMLFormatter(super.getHighlightPreTag(), super.getHighlightPostTag());
        QueryScorer scorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(formatter, scorer);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, this.getFragmentSize());
        highlighter.setTextFragmenter(fragmenter);
        List<SearchableDocument> documents = new ArrayList<>(size);
        int end = docs.scoreDocs.length;
        for (int i = offset; i < end; i++) {
            ScoreDoc doc = docs.scoreDocs[i];
            int docId = doc.doc;
            Document d = searcher.doc(docId);
            SearchableDocument r = new SearchableDocument();
            r.id = Long.parseLong(d.get(SearchableDocument.FIELD_ID));
            r.type = d.get(SearchableDocument.FIELD_TYPE);
            r.url = d.get(SearchableDocument.FIELD_URL);
            r.name = highlight(highlighter, d, SearchableDocument.FIELD_NAME);
            r.content = highlight(highlighter, d, SearchableDocument.FIELD_CONTENT);
            documents.add(r);
        }
        return new Hits((int) docs.totalHits.value, offset, documents);
    }

    private String highlight(Highlighter highlighter, Document d, String field) throws Exception {
        String s = d.get(field);
        TokenStream tokenStream = this.analyzer.tokenStream(field, s);
        String fr = highlighter.getBestFragments(tokenStream, s, 3, "...");
        if (fr.isEmpty()) {
            fr = s;
            if (fr.length() > this.getFragmentSize()) {
                fr = fr.substring(0, this.getFragmentSize()) + "...";
            }
        }
        return fr;
    }

    // search in name and content:
    private Query buildMultiQuery(String[] qs) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String s : qs) {
            builder.add(buildSingleQuery("name", s), Occur.SHOULD);
            builder.add(buildSingleQuery("content", s), Occur.SHOULD);
        }
        return builder.build();
    }

    private Query buildSingleQuery(String field, String s) {
        if (s.chars().allMatch(n -> n < 128)) {
            // all ascii:
            return new TermQuery(new Term(field, s));
        }
        if (s.length() == 1) {
            return new TermQuery(new Term(field, s));
        }
        return new PhraseQuery(field, chars(s));
    }

    private String[] chars(String s) {
        String[] ss = new String[s.length()];
        for (int i = 0; i < s.length(); i++) {
            ss[i] = String.valueOf(s.charAt(i));
        }
        return ss;
    }

    private Document toLuceneDocument(SearchableDocument sd) throws Exception {
        Document doc = new Document();
        doc.add(new StringField(SearchableDocument.FIELD_ID, String.valueOf(sd.id), Field.Store.YES));
        doc.add(new LongPoint(SearchableDocument.FIELD_PUBLISH_AT, sd.publishAt));
        doc.add(new StringField(SearchableDocument.FIELD_TYPE, sd.type, Field.Store.YES));
        doc.add(new TextField(SearchableDocument.FIELD_NAME, sd.name, Field.Store.YES));
        doc.add(new TextField(SearchableDocument.FIELD_CONTENT, sd.content, Field.Store.YES));
        doc.add(new StoredField(SearchableDocument.FIELD_URL, sd.url));
        return doc;
    }

    @Override
    protected void indexSearchableDocuments(SearchableDocument... documents) throws Exception {
        logger.info("add searchable documents...");
        for (var document : documents) {
            this.indexWriter.addDocument(toLuceneDocument(document));
        }
        this.indexWriter.commit();
    }

    @Override
    public void unindexSearchableDocument(long id) throws Exception {
        logger.info("remove searchable document...");
        this.indexWriter.deleteDocuments(new Term("id", String.valueOf(id)));
        this.indexWriter.commit();
    }

    @Override
    public void removeAllIndex() throws Exception {
        logger.warn("will remove ALL index at {}!", this.indexDir);
        this.indexWriter.deleteAll();
        this.indexWriter.commit();
        logger.warn("ALL index removed at {}!", this.indexDir);
    }
}
