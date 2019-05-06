package com.itranswarp.service;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.Role;
import com.itranswarp.model.AbstractEntity;
import com.itranswarp.model.LocalAuth;
import com.itranswarp.model.OAuth;
import com.itranswarp.model.User;
import com.itranswarp.oauth.OAuthAuthentication;
import com.itranswarp.util.HashUtil;
import com.itranswarp.util.IdUtil;
import com.itranswarp.util.JsonUtil;
import com.itranswarp.util.RandomUtil;
import com.itranswarp.warpdb.PagedResults;

@Component
public class UserService extends AbstractService<User> {

	static final String KEY_USERS = "__users__";

	public List<User> getUsersFromCache(Object... ids) {
		var kvs = this.redisService.hmget(KEY_USERS, ids);
		kvs.stream().map(kv -> {
			if (kv.hasValue()) {
				String json = kv.getValue();
				return JsonUtil.readJson(json, User.class);
			} else {
				String id = kv.getKey();
				User user = getById(Long.valueOf(id));
				this.redisService.hset(KEY_USERS, id, user);
				return user;
			}
		});
		return null;
	}

	public User getEnabledUserById(Long id) {
		User user = this.getById(id);
		if (user.lockedUntil > System.currentTimeMillis()) {
			return null;
		}
		return user;
	}

	public User getUserFromCache(Long id) {
		User user = this.redisService.hget(KEY_USERS, id, User.class);
		if (user == null) {
			user = getById(id);
			this.redisService.hset(KEY_USERS, id, user);
		}
		return user;
	}

	public List<User> getUsersByIds(long... ids) {
		if (ids.length == 0) {
			return List.of();
		}
		if (ids.length > 100) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "id", "Too many ids.");
		}
		StringJoiner sj = new StringJoiner(" OR ");
		for (int i = 0; i < ids.length; i++) {
			sj.add("id=?");
		}
		String where = sj.toString();
		Object[] params = Arrays.stream(ids).mapToObj(id -> id).toArray();
		return this.db.from(User.class).where(where, params).list();
	}

	public List<User> searchUsers(String q) {
		return this.db.from(User.class).where("name LIKE ?", q + "%").orderBy("id").desc().limit(ITEMS_PER_PAGE).list();
	}

	public PagedResults<User> getUsers(int pageIndex) {
		return this.db.from(User.class).orderBy("id").desc().list(pageIndex, ITEMS_PER_PAGE);
	}

	public List<User> getUsersByRole(Role role, int maxResults) {
		return this.db.from(User.class).where("role = ?", role).orderBy("id").limit(maxResults).list();
	}

	public User fetchUserByEmail(String email) {
		return this.db.from(User.class).where("email = ?", email).first();
	}

	@Transactional
	public OAuth getOAuth(String authProviderId, OAuthAuthentication authentication) {
		OAuth oauth = this.db.from(OAuth.class)
				.where("authProviderId = ? AND authId = ?", authProviderId, authentication.getAuthenticationId())
				.first();
		if (oauth == null) {
			return createOAuthUser(authProviderId, authentication);
		}
		oauth.authToken = authentication.getAccessToken();
		oauth.expiresAt = System.currentTimeMillis() + authentication.getExpires().toMillis();
		this.db.update(oauth);
		return oauth;
	}

	OAuth createOAuthUser(String authProviderId, OAuthAuthentication authentication) {
		if ("local".equals(authProviderId)) {
			throw new RuntimeException("Invalid provider: " + authProviderId);
		}
		User user = new User();
		user.id = IdUtil.nextId();
		user.email = user.id + "@" + authProviderId.toLowerCase();
		user.name = checkName(authentication.getName());
		user.imageUrl = checkImageUrl(authentication.getImageUrl());
		user.role = Role.SUBSCRIBER;

		OAuth auth = new OAuth();
		auth.userId = user.id;
		auth.authProviderId = authProviderId;
		auth.authId = authentication.getAuthenticationId();
		auth.authToken = authentication.getAccessToken();
		auth.expiresAt = System.currentTimeMillis() + authentication.getExpires().toMillis();

		this.db.insert(user);
		this.db.insert(auth);
		return auth;
	}

	@Transactional
	public User createLocalUser(String email, String password, String name, String imageUrl) {
		User user = new User();
		user.id = IdUtil.nextId();
		user.email = checkEmail(email);
		user.name = checkName(name);
		user.imageUrl = checkImageUrl(imageUrl);
		user.role = Role.SUBSCRIBER;
		LocalAuth auth = new LocalAuth();
		auth.userId = user.id;
		auth.salt = RandomUtil.createRandomString(AbstractEntity.VAR_CHAR_HASH);
		auth.passwd = HashUtil.hmacSha256(password, auth.salt);
		this.db.insert(user);
		this.db.insert(auth);
		return user;
	}

	public OAuth fetchOAuthById(String authProviderId, Long id) {
		return this.db.from(OAuth.class).where("id = ? AND authProviderId = ?", id, authProviderId).first();
	}

	public LocalAuth fetchLocalAuthById(Long id) {
		return this.db.fetch(LocalAuth.class, id);

	}

	public LocalAuth fetchLocalAuthByUserId(Long userId) {
		return this.db.from(LocalAuth.class).where("userId = ?", userId).first();

	}

	@Transactional
	public User updateUserRole(Long id, Role role) {
		User user = getById(id);
		if (user.role == Role.ADMIN) {
			throw new ApiException(ApiError.OPERATION_FAILED, "role", "Could not change admin role.");
		}
		if (user.role != role) {
			user.role = role;
			this.db.updateProperties(user, "role");
		}
		return user;
	}

	@Transactional
	public User updateUserLockedUntil(Long id, long ts) {
		User user = getById(id);
		if (user.role == Role.ADMIN) {
			throw new ApiException(ApiError.OPERATION_FAILED, null, "Could not lock admin user.");
		}
		user.lockedUntil = ts;
		this.db.updateProperties(user, "lockedUntil");
		return user;
	}

	@Transactional
	public void lockUser(User user, int days) {
		user.lockedUntil = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
		this.db.updateProperties(user, "lockedUntil");
	}

	public void clearUserFromCache(Long id) {
		this.redisService.hdel(KEY_USERS, id);
	}

	String checkName(String name) {
		if (name == null) {
			return "(unamed)";
		}
		name = name.strip();
		if (name.length() > AbstractEntity.VAR_CHAR_NAME) {
			name = name.substring(0, AbstractEntity.VAR_CHAR_NAME);
		}
		return name;
	}

	String checkEmail(String value) {
		if (value == null) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "email", "Invalid email address.");
		}
		String email = value.strip().toLowerCase();
		if (email.length() > AbstractEntity.VAR_CHAR_EMAIL) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "email", "Email address is too long.");
		}
		if (!EmailValidator.getInstance(true).isValid(email)) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "email", "Invalid email address.");
		}
		return email;
	}

	String checkImageUrl(String url) {
		if (url == null) {
			return "about:blank";
		}
		url = url.strip();
		if (url.isEmpty()) {
			return "about:blank";
		}
		if (url.length() > AbstractEntity.VAR_CHAR_URL) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "url", "url is too long.");
		}
		if (url.startsWith("/") || url.startsWith("https://") || url.startsWith("http://")) {
			return url;
		}
		throw new ApiException(ApiError.PARAMETER_INVALID, "url", "Invalid url.");
	}
}
