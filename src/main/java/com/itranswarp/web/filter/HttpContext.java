package com.itranswarp.web.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.Role;
import com.itranswarp.model.User;
import com.itranswarp.util.HttpUtil;

public class HttpContext implements AutoCloseable {

    static final Logger logger = LoggerFactory.getLogger(HttpContext.class);

    static final ThreadLocal<HttpContext> CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    public final User user;
    public final long timestamp;
    public final HttpServletRequest request;
    public final HttpServletResponse response;
    public final String scheme;
    public final String host;
    public final String path;
    public final String url;
    public final String ip;

    HttpContext(User user, HttpServletRequest request, HttpServletResponse response, String ip) {
        this.user = user;
        this.timestamp = System.currentTimeMillis();
        this.request = request;
        this.response = response;
        this.scheme = HttpUtil.getScheme(request);
        this.host = request.getServerName().toLowerCase();
        this.path = request.getRequestURI();
        String query = request.getQueryString();
        this.url = this.scheme + "://" + this.host + this.path + (query == null ? "" : "?" + query);
        this.ip = ip;
        logger.info("process new http request from {}: {} {}...", ip, request.getMethod(), this.url);
        CONTEXT_THREAD_LOCAL.set(this);
    }

    public static HttpContext getContext() {
        return CONTEXT_THREAD_LOCAL.get();
    }

    @SuppressWarnings("resource")
    public static long getTimestamp() {
        return CONTEXT_THREAD_LOCAL.get().timestamp;
    }

    @SuppressWarnings("resource")
    public static User getCurrentUser() {
        return CONTEXT_THREAD_LOCAL.get().user;
    }

    public static User getRequiredCurrentUser() {
        User user = getCurrentUser();
        if (user == null) {
            throw new ApiException(ApiError.AUTH_SIGNIN_REQUIRED, null, "Need signin first.");
        }
        return user;
    }

    public static void checkRole(Role expectedRole) {
        User user = getRequiredCurrentUser();
        if (user.role.value > expectedRole.value) {
            throw new ApiException(ApiError.PERMISSION_DENIED);
        }
    }

    @Override
    public void close() {
        CONTEXT_THREAD_LOCAL.remove();
    }
}
