package com.itranswarp.web.filter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;

import com.itranswarp.bean.SessionCookieBean;
import com.itranswarp.enums.Role;
import com.itranswarp.model.EthAuth;
import com.itranswarp.model.LocalAuth;
import com.itranswarp.model.OAuth;
import com.itranswarp.model.User;
import com.itranswarp.oauth.OAuthProviders;
import com.itranswarp.service.AntiSpamService;
import com.itranswarp.service.EncryptService;
import com.itranswarp.service.RedisRateLimiter;
import com.itranswarp.service.UserService;
import com.itranswarp.util.CookieUtil;
import com.itranswarp.util.HttpUtil;

@Component
public class GlobalFilterRegistrationBean extends FilterRegistrationBean<Filter> {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${spring.security.rate-limit.error-code:429}")
    int rateLimitErrorCode = 429;

    @Value("${spring.security.rate-limit.limit:3}")
    int rateLimit = 3;

    @Value("${spring.security.rate-limit.burst:10}")
    int rateLimitBurst = 10;

    @Autowired
    EncryptService encryptService;

    @Autowired
    AntiSpamService antiSpamService;

    @Autowired
    OAuthProviders oauthProviders;

    @Autowired
    UserService userService;

    @Autowired
    RedisRateLimiter rateLimiter;

    String serverId = getServerId();

    @PostConstruct
    public void init() {
        setOrder(100);
        setUrlPatterns(List.of("/*"));
        setFilter(new GlobalFilter());
    }

    private String getServerId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.warn("could not get hostname.", e);
        }
        return "localhost";
    }

    class GlobalFilter implements Filter {

        @Override
        public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) resp;
            request.setCharacterEncoding("UTF-8");
            response.setHeader("X-Server-ID", serverId);
            final String ip = HttpUtil.getIPAddress(request);
            if (antiSpamService.isSpamIp(ip)) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
            final String path = request.getRequestURI();
            // check rate limit but except static file:
            if (!path.startsWith("/static/") && !path.startsWith("/files/")) {
                int remaining = rateLimiter.getRateLimit("www", ip, rateLimit, rateLimitBurst);
                response.setIntHeader("X-RateLimit-Limit", rateLimit);
                if (remaining <= 0) {
                    response.setIntHeader("X-RateLimit-Remaining", 0);
                    response.setStatus(rateLimitErrorCode);
                    return;
                }
                response.setIntHeader("X-RateLimit-Remaining", remaining - 1);
            }
            User user = null;
            String cookieStr = CookieUtil.findSessionCookie(request);
            if (cookieStr != null) {
                SessionCookieBean session = CookieUtil.decodeSessionCookie(cookieStr);
                if (session == null) {
                    CookieUtil.deleteSessionCookie(request, response);
                } else {
                    if ("local".equals(session.authProvider)) {
                        LocalAuth auth = userService.fetchLocalAuthById(session.id);
                        if (auth != null && session.validate(auth.passwd, encryptService.getSessionHmacKey())) {
                            user = userService.getEnabledUserById(auth.userId);
                        } else {
                            CookieUtil.deleteSessionCookie(request, response);
                        }
                    } else if ("eth".equals(session.authProvider)) {
                        EthAuth auth = userService.fetchEthAuthById(session.id);
                        if (auth != null && session.validate(auth.address, encryptService.getSessionHmacKey())) {
                            user = userService.getEnabledUserById(auth.userId);
                        } else {
                            CookieUtil.deleteSessionCookie(request, response);
                        }
                    } else {
                        OAuth auth = userService.fetchOAuthById(session.authProvider, session.id);
                        if (auth != null && session.validate(auth.authToken, encryptService.getSessionHmacKey())) {
                            user = userService.getEnabledUserById(auth.userId);
                        } else {
                            CookieUtil.deleteSessionCookie(request, response);
                        }
                    }
                }
            }
            String uri = request.getRequestURI();
            if (!uri.startsWith("/files/")) {
                response.setCharacterEncoding("UTF-8");
            }
            if (uri.startsWith("/manage/")) {
                if (user == null) {
                    response.sendRedirect("/auth/signin");
                    return;
                }
                if (user.role.value > Role.CONTRIBUTOR.value) {
                    logger.info("prevent access /manage/ for user {}.", user);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }
            try (HttpContext context = new HttpContext(user, request, response, ip)) {
                chain.doFilter(req, resp);
            }
        }
    }
}
