package com.itranswarp.web.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.redis.RedisService;
import com.itranswarp.util.JsonUtil;
import com.itranswarp.web.filter.HttpContext;

@RestController
@RequestMapping("/api/external")
public class ExtApiController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

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

	@Autowired
	RedisService redisService;

	private HttpClient httpClient;

	private String REMOTE_CODE_RUNNER_TIMEOUT = "{\"timeout\":true,\"error\":false,\"truncated\":false,\"output\":\"\"}";

	@PostConstruct
	public void init() {
		Executor executor = new ThreadPoolExecutor(1, this.remoteCodeRunnerMaxConcurrent, 60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		this.httpClient = HttpClient.newBuilder() // builder
				.connectTimeout(Duration.ofSeconds(2)) // connect timeout
				.executor(executor).build();
	}

	// remote code running ////////////////////////////////////////////////////

	@PostMapping("/remoteCodeRun")
	@ResponseBody
	public DeferredResult<String> remoteCodeRun(@RequestBody RemoteCodeRunInput input) {
		if (!this.remoteCodeRunnerEnabled) {
			throw new ApiException(ApiError.OPERATION_FAILED, null, "Remote code running service is not enabled.");
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
		CompletableFuture<HttpResponse<String>> cf = this.httpClient.sendAsync(request,
				HttpResponse.BodyHandlers.ofString());
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
