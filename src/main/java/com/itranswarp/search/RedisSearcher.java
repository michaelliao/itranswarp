package com.itranswarp.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.itranswarp.redis.SyncCommandCallback;
import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RedisModulesCommands;
import com.redis.lettucemod.search.CreateOptions;
import com.redis.lettucemod.search.Document;
import com.redis.lettucemod.search.Field;
import com.redis.lettucemod.search.Language;
import com.redis.lettucemod.search.SearchOptions;
import com.redis.lettucemod.search.SearchOptions.NumericFilter;
import com.redis.lettucemod.search.SearchOptions.Summarize;
import com.redis.lettucemod.search.SearchResults;

import io.lettuce.core.RedisCommandExecutionException;

@Component
public class RedisSearcher extends AbstractSearcher {

    static final String REDISEARCH_INDEX_NAME = "idx:doc";

    @Value("${spring.search.redisearch.default-language:CHINESE}")
    String defaultLanguage;

    @Autowired
    RedisModulesClient redisClient;

    @Override
    protected IndexStatus doInitIndex() throws Exception {
        logger.warn("will init index!");
        return executeSearch(commands -> {
            try {
                CreateOptions.Builder<String, String> cob = CreateOptions.builder();
                cob.defaultLanguage(Language.valueOf(this.defaultLanguage));
                String result = commands.create(REDISEARCH_INDEX_NAME, cob.build(),
                        // fields:
                        Field.numeric("id").noIndex().build(), Field.numeric("publishAt").build(), Field.tag("type").build(),
                        Field.text("name").weight(10).build(), Field.text("description").weight(5).build(), Field.text("content").weight(1).build(),
                        Field.text("url").noIndex().build());
                logger.info("FT.CREATE language {}, index {}: {}", REDISEARCH_INDEX_NAME, this.defaultLanguage, result);
                return IndexStatus.CREATED;
            } catch (RedisCommandExecutionException e) {
                if ("Index already exists".equals(e.getMessage())) {
                    logger.warn("FT: index already exists.");
                    return IndexStatus.EXIST;
                } else {
                    logger.error("init index error: {}", e.getMessage());
                }
            } catch (Exception e) {
                logger.error("init index error.", e);
            }
            return IndexStatus.FAILED;
        });
    }

    @Override
    public String getEngineName() {
        return "RediSearch";
    }

    @Override
    protected Hits search(List<String> qs, int maxResults, long timestamp) throws Exception {
        SearchResults<String, String> sr = executeSearch(commands -> {
            NumericFilter<String, String> publishFilter = new NumericFilter<>("publishAt", 0, System.currentTimeMillis());

            Summarize<String, String> sm = new Summarize<>();
            sm.setFields(List.of("content"));
            sm.setLength(64);
            sm.setFrags(3);
            SearchOptions.Builder<String, String> builder = SearchOptions.builder();
            builder.limit(SearchOptions.limit(0, 20));

            builder.summarize(sm);
            builder.filter(publishFilter);
            String q = String.join("|", qs);
            logger.info("search query: {}", q);
            return commands.search(REDISEARCH_INDEX_NAME, q, builder.build());
        });
        if (logger.isDebugEnabled()) {
            logger.debug("search results: count = {}", sr.getCount());
            sr.forEach(doc -> {
                logger.debug("result id = {}: type = {}, name = {}, url = {}, content = {}", doc.getId(), doc.get("type"), doc.get("name"), doc.get("url"),
                        doc.get("content"));
            });
        }

        if (sr.isEmpty()) {
            return Hits.empty();
        }
        List<SearchableDocument> documents = new ArrayList<>(sr.size());
        for (Document<String, String> doc : sr) {
            var sd = new SearchableDocument();
            sd.type = doc.get("type");
            sd.name = doc.get("name");
            sd.url = doc.get("url");
            String content = doc.get("content");
            if (content.length() > 300) {
                content = content.substring(0, 300) + "...";
            }
            sd.content = content;
            documents.add(sd);
        }
        return new Hits((int) sr.getCount(), documents);
    }

    @Override
    protected void indexSearchableDocuments(List<SearchableDocument> documents) throws Exception {
        logger.info("add searchable documents...");
        executeSearch(commands -> {
            for (SearchableDocument doc : documents) {
                commands.hset("doc:" + doc.id,
                        Map.of("type", doc.type, "name", doc.name, "content", doc.content, "publishAt", String.valueOf(doc.publishAt), "url", doc.url));
            }
            logger.info("{} docs indexed.", documents.size());
            return documents.size();
        });
    }

    @Override
    public boolean unindexSearchableDocument(long id) throws Exception {
        logger.info("remove searchable document {}...", id);
        executeSearch(commands -> {
            long n = commands.del("doc:" + id);
            return n == 1;
        });
        return true;
    }

    @Override
    protected IndexStatus doRemoveIndex() throws Exception {
        logger.warn("will remove index!");
        return executeSearch(commands -> {
            try {
                String result = commands.dropindexDeleteDocs(REDISEARCH_INDEX_NAME);
                logger.info("FT.DROP index {}: {}", REDISEARCH_INDEX_NAME, result);
                return IndexStatus.REMOVED;
            } catch (Exception e) {
                logger.error("init index error.", e);
                return IndexStatus.FAILED;
            }
        });
    }

    <T> T executeSearch(SyncCommandCallback<T> callback) {
        try (StatefulRedisModulesConnection<String, String> connection = this.redisClient.connect()) {
            connection.setAutoFlushCommands(true);
            RedisModulesCommands<String, String> commands = connection.sync();
            return callback.doInConnection(commands);
        }
    }
}
