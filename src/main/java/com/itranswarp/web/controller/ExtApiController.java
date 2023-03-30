package com.itranswarp.web.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import com.itranswarp.common.AbstractService;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.model.User;
import com.itranswarp.redis.RedisService;
import com.itranswarp.util.JsonUtil;
import com.itranswarp.web.filter.HttpContext;

@RestController
@RequestMapping("/api/external")
public class ExtApiController extends AbstractService {

    @Value("${spring.external.remote-code-runner.enabled:false}")
    boolean remoteCodeRunnerEnabled;

    @Value("${spring.external.remote-code-runner.url:}")
    String remoteCodeRunnerUrl;

    @Value("${spring.external.remote-code-runner.languages:java,python}")
    Set<String> remoteCodeRunnerLanguages;

    @Value("${spring.external.remote-code-runner.wait:15}")
    int remoteCodeRunnerWait;

    @Value("${spring.external.remote-code-runner.timeout:10}")
    int remoteCodeRunnerTimeout;

    @Value("${spring.external.remote-code-runner.max-concurrent:10}")
    int remoteCodeRunnerMaxConcurrent;

    @Value("${spring.external.chat-gpt.enabled:false}")
    boolean chatGptEnabled;

    @Value("${spring.external.chat-gpt.url:https://api.openai.com/v1/chat/completions}")
    String chatGptUrl;

    @Value("${spring.external.chat-gpt.timeout:30}")
    int chatGptTimeout;

    @Value("${spring.external.chat-gpt.api-key:}")
    String chatGptApiKey;

    @Value("${spring.external.chat-gpt.model:gpt-3.5-turbo}")
    String chatGptModel;

    @Value("${spring.external.chat-gpt.prompt:}")
    String chatGptPrompt;

    @Value("${spring.external.chat-gpt.rate-limit:100}")
    long chatGptRateLimit;

    @Autowired
    RedisService redisService;

    @Autowired
    ZoneId zoneId;

    private HttpClient httpClient;

    private String REMOTE_CODE_RUNNER_TIMEOUT = "{\"timeout\":true,\"error\":false,\"truncated\":false,\"output\":\"\"}";

    @PostConstruct
    public void init() {
        Executor executor = new ThreadPoolExecutor(1, this.remoteCodeRunnerMaxConcurrent, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        this.httpClient = HttpClient.newBuilder() // builder
                .connectTimeout(Duration.ofSeconds(2)) // connect timeout
                .executor(executor).build();
    }

    // call chat-gpt //////////////////////////////////////////////////////////

    String gptRateLimitKey(User user) {
        return "_cgpt_" + ZonedDateTime.now(this.zoneId).toLocalDate() + "_" + user.id;
    }

    boolean isGptRateLimitExceeded(long increment) {
        User user = HttpContext.getCurrentUser();
        if (user == null) {
            return true;
        }
        String key = gptRateLimitKey(user);
        String value = redisService.executeSync(command -> {
            return command.get(key);
        });
        return value != null && Long.valueOf(value) > this.chatGptRateLimit;
    }

    @GetMapping("/gpt/ratelimit")
    public Map<String, Boolean> chatGptRateLimit() {
        return Map.of("result", !this.chatGptEnabled || isGptRateLimitExceeded(0));
    }

    @PostMapping("/gpt")
    @ResponseBody
    public DeferredResult<String> chatGpt(@RequestBody ChatGptInput input) {
        if (!this.chatGptEnabled) {
            throw new ApiException(ApiError.OPERATION_FAILED, null, "ChatGPT service is not enabled.");
        }
        if (this.chatGptUrl == null || this.chatGptUrl.isEmpty()) {
            throw new ApiException(ApiError.OPERATION_FAILED, null, "ChatGPT URL is not set.");
        }
        if (input.content == null || input.content.isBlank() || input.content.length() > 500) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "content", "Invalid content.");
        }
        User user = HttpContext.getCurrentUser();
        if (user == null) {
            throw new ApiException(ApiError.AUTH_SIGNIN_REQUIRED, null, "Signin required.");
        }
        String key = gptRateLimitKey(user);
        // check rate limit:
        long times = redisService.executeSync(command -> {
            return command.incr(key);
        });
        if (times > this.chatGptRateLimit) {
            throw new ApiException(ApiError.RATE_LIMIT, null, "Maximum reached.");
        }
        List<Map<String, String>> messages = new ArrayList<>(2);
        if (this.chatGptPrompt != null && !this.chatGptPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", this.chatGptPrompt));
        }
        messages.add(Map.of("role", "user", "content", input.content.strip()));
        Map<String, Object> inputData = Map.of("model", this.chatGptModel, "messages", messages);
        byte[] jsonData = JsonUtil.writeJsonAsBytes(inputData);
        Builder builder = HttpRequest.newBuilder(URI.create(this.chatGptUrl)) // url
                .timeout(Duration.ofSeconds(this.chatGptTimeout)) // timeout
                .header("Content-Type", "application/json"); // json
        if (this.chatGptApiKey != null && !this.chatGptApiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + this.chatGptApiKey); // auth
        }
        HttpRequest request = builder.POST(BodyPublishers.ofByteArray(jsonData)).version(Version.HTTP_1_1).build();
        CompletableFuture<HttpResponse<String>> cf = this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        DeferredResult<String> dr = new DeferredResult<>(Long.valueOf(30_000), () -> {
            return JsonUtil.writeJson(Map.of("error", ApiError.TIMEOUT));
        });
        cf.thenAccept(resp -> {
            String gptResp = resp.body();
            logger.info("gpt response: {}", gptResp);
            ChatGptOutput gptOutput = JsonUtil.readJson(gptResp, ChatGptOutput.class);
            if (gptOutput.choices != null && !gptOutput.choices.isEmpty()) {
                dr.setResult(JsonUtil.writeJson(Map.of("content", gptOutput.choices.get(0).message.content)));
            } else {
                dr.setErrorResult(new ApiException(ApiError.INTERNAL_SERVER_ERROR));
            }
        });
        cf.exceptionally(e -> {
            logger.error("call chat gpt error!", e);
            dr.setErrorResult(new ApiException(ApiError.INTERNAL_SERVER_ERROR));
            return null;
        });
        return dr;
    }

    // remote code running ////////////////////////////////////////////////////

    @PostMapping("/remoteCodeRun")
    @ResponseBody
    public DeferredResult<String> remoteCodeRun(@RequestBody RemoteCodeRunInput input) {
        if (!this.remoteCodeRunnerEnabled) {
            throw new ApiException(ApiError.OPERATION_FAILED, null, "Remote code running service is not enabled.");
        }
        User user = HttpContext.getCurrentUser();
        if (user == null) {
            throw new ApiException(ApiError.AUTH_SIGNIN_REQUIRED, null, "Signin required.");
        }
        if (input.language == null || !this.remoteCodeRunnerLanguages.contains(input.language)) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "language", "Invalid language.");
        }
        if (input.code == null || input.code.isBlank() || input.code.length() > 4096) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "code", "Invalid code.");
        }
        @SuppressWarnings("resource")
        String key = "_rcr_" + HttpContext.getContext().ip;
        boolean canRun = redisService.executeSync(command -> {
            if (command.get(key) == null) {
                command.setex(key, this.remoteCodeRunnerWait, "ok");
                return true;
            } else {
                return false;
            }
        });
        if (!canRun) {
            throw new ApiException(ApiError.RATE_LIMIT, null, "Rate limit.");
        }
        byte[] jsonData = JsonUtil.writeJsonAsBytes(input);
        HttpRequest request = HttpRequest.newBuilder(URI.create(this.remoteCodeRunnerUrl)) // url
                .timeout(Duration.ofSeconds(this.remoteCodeRunnerTimeout)) // timeout
                .header("Content-Type", "application/json") // json
                .POST(BodyPublishers.ofByteArray(jsonData)).version(Version.HTTP_1_1).build();
        CompletableFuture<HttpResponse<String>> cf = this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        DeferredResult<String> dr = new DeferredResult<>(Long.valueOf(this.remoteCodeRunnerTimeout * 1000L), () -> {
            redisService.executeSync(command -> {
                command.setex(key, this.remoteCodeRunnerTimeout * 6, "ok");
                return "";
            });
            return REMOTE_CODE_RUNNER_TIMEOUT;
        });
        cf.thenAccept(resp -> {
            dr.setResult(resp.body());
        });
        cf.exceptionally(e -> {
            logger.error("call remote code runner error!", e);
            dr.setErrorResult(new ApiException(ApiError.INTERNAL_SERVER_ERROR));
            return null;
        });
        return dr;
    }

    // static bean class //////////////////////////////////////////////////////

    public static class ChatGptInput {
        public String content;
    }

    public static class ChatGptOutput {
        public List<Choice> choices;

        public static class Choice {
            public Message message;
        }

        public static class Message {
            public String role;
            public String content;
        }
    }

    public static class RemoteCodeRunInput {
        public String language;
        public String code;
    }

    public static class RemoteCodeRunResult {
        public boolean timeout;
        public boolean error;
        public boolean truncated;
        public String output;
    }
}
