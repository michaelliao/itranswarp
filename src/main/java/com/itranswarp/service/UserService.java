package com.itranswarp.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

	static final String KEY_USERS = "_users";

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

	public User getUserFromCache(Long id) {
		User user = this.redisService.hget(KEY_USERS, id, User.class);
		if (user == null) {
			user = getById(id);
			this.redisService.hset(KEY_USERS, id, user);
		}
		return user;
	}

	public PagedResults<User> getUsers(int pageIndex) {
		return this.db.from(User.class).orderBy("id").desc().list(pageIndex, ITEMS_PER_PAGE);
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
		user.name = this.checkName(authentication.getName());
		user.imageUrl = this.checkUrl(authentication.getImageUrl());
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
		user.email = this.checkEmail(email);
		user.name = this.checkName(name);
		user.imageUrl = this.checkUrl(imageUrl);
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
	public void updateUserRole(User user, Role role) {
		user.role = role;
		this.db.updateProperties(user, "role");
		clearUserFromCache(user.id);
	}

	@Transactional
	public void lockUser(User user, int days) {
		user.lockedUntil = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
		this.db.updateProperties(user, "lockedUntil");
		clearUserFromCache(user.id);
	}

	void clearUserFromCache(Long id) {
		this.redisService.hdel(KEY_USERS, id);
	}

}
