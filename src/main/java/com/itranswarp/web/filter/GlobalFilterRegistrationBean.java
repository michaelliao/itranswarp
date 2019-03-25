package com.itranswarp.web.filter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;

import com.itranswarp.bean.SessionCookieBean;
import com.itranswarp.enums.Role;
import com.itranswarp.model.LocalAuth;
import com.itranswarp.model.OAuth;
import com.itranswarp.model.User;
import com.itranswarp.oauth.OAuthProviders;
import com.itranswarp.service.EncryptService;
import com.itranswarp.service.UserService;
import com.itranswarp.util.CookieUtil;

@Component
public class GlobalFilterRegistrationBean extends FilterRegistrationBean<Filter> {

	final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	EncryptService encryptService;

	@Autowired
	OAuthProviders oauthProviders;

	@Autowired
	UserService userService;

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
		public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
				throws IOException, ServletException {
			HttpServletRequest request = (HttpServletRequest) req;
			HttpServletResponse response = (HttpServletResponse) resp;
			request.setCharacterEncoding("UTF-8");
			response.setHeader("X-Server-ID", serverId);
			User user = null;
			String cookieStr = CookieUtil.findSessionCookie(request);
			if (cookieStr != null) {
				SessionCookieBean session = CookieUtil.decodeSessionCookie(cookieStr);
				if (session == null) {
					CookieUtil.deleteSessionCookie(request, response);
				} else {
					if ("local".equals(session.authProvider)) {
						LocalAuth auth = userService.fetchLocalAuthById(session.id);
						if (session.validate(auth.passwd, encryptService.getSessionHmacKey())) {
							user = userService.getById(auth.userId);
						}
					} else {
						OAuth auth = userService.fetchOAuthById(session.authProvider, session.id);
						if (session.validate(auth.authToken, encryptService.getSessionHmacKey())) {
							user = userService.getById(auth.userId);
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
					response.sendRedirect("/auth/");
					return;
				}
				if (user.role.value > Role.CONTRIBUTOR.value) {
					logger.info("prevent access /manage/ for user {}.", user);
					response.sendError(HttpServletResponse.SC_FORBIDDEN);
					return;
				}
			}
			try (HttpContext context = new HttpContext(user, request, response)) {
				chain.doFilter(req, resp);
			}
		}
	}
}
