package com.itranswarp.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.Role;
import com.itranswarp.model.AbstractEntity;
import com.itranswarp.model.AbstractSortableEntity;
import com.itranswarp.model.User;
import com.itranswarp.redis.RedisService;
import com.itranswarp.util.ClassUtil;
import com.itranswarp.warpdb.WarpDb;

@Transactional
public class AbstractService<T extends AbstractEntity> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final int ITEMS_PER_PAGE = 10;

	private Class<T> entityClass;

	@Autowired
	protected WarpDb db;

	@Autowired
	protected RedisService redisService;

	public AbstractService() {
		this.entityClass = ClassUtil.getParameterizedType(this.getClass());
	}

	public T getById(Long id) {
		T t = this.db.fetch(entityClass, id);
		if (t == null) {
			throw new ApiException(ApiError.ENTITY_NOT_FOUND, entityClass.getSimpleName(),
					entityClass.getSimpleName() + " not found");
		}
		return t;
	}

	public T fetchById(Long id) {
		return this.db.fetch(entityClass, id);
	}

	protected void checkPermission(User user, long entityUserId) {
		if (user.role != Role.ADMIN && user.id != entityUserId) {
			throw new ApiException(ApiError.PERMISSION_DENIED);
		}
	}

	protected void sortEntities(List<? extends AbstractSortableEntity> entities, List<Long> ids) {
		if (ids == null) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "ids", "Invalid ids.");
		}
		if (entities.size() != ids.size() || entities.size() != new HashSet<>(ids).size()) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "ids", "Invalid ids.");
		}
		entities.forEach(entity -> {
			int n = ids.indexOf(entity.id);
			if (n == (-1)) {
				throw new ApiException(ApiError.PARAMETER_INVALID, "ids", "Invalid category ids.");
			}
			entity.displayOrder = n;
		});
		entities.forEach(entity -> {
			this.db.updateProperties(entity, "displayOrder");
		});
	}

	protected long checkPublishAt(Long value) {
		if (value == null || value < 0L) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "publishAt", "Invalid publishAt.");
		}
		return value;
	}

	protected String checkEmail(String value) {
		String email = checkString("email", AbstractEntity.VAR_CHAR_EMAIL, value);
		if (!EmailValidator.getInstance(true).isValid(email)) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "email", "Invalid email address.");
		}
		return email;
	}

	protected String checkPassword(String value) {
		if (value == null || value.length() != AbstractEntity.VAR_CHAR_HASH) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "password", "Invalid password.");
		}
		Matcher matcher = PATTERN_HASHED_PASSWORD.matcher(value);
		if (!matcher.matches()) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "password", "Invalid password.");
		}
		return value;
	}

	protected String checkName(String value) {
		return checkString("name", AbstractEntity.VAR_CHAR_NAME, value);
	}

	protected String checkIcon(String value) {
		return checkString("icon", AbstractEntity.VAR_ENUM, value);
	}

	protected String checkDescription(String value) {
		return checkString("description", AbstractEntity.VAR_CHAR_DESCRIPTION, value);
	}

	protected String checkText(String value) {
		return checkString("text", 65535, value);
	}

	protected String checkTag(String value) {
		if (value == null) {
			return "";
		}
		Matcher matcher = PATTERN_TAG.matcher(value);
		if (!matcher.matches()) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "tag", "Invalid tag.");
		}
		return checkString("tag", AbstractEntity.VAR_ENUM, value);
	}

	protected String checkTags(String value) {
		if (value == null) {
			return "";
		}
		String[] ss = Arrays.stream(value.replaceAll("\\s+", " ").split("\\s?[\\,\\;]+\\s?")).map(String::strip)
				.filter(s -> !s.isEmpty()).map(this::checkTag).toArray(String[]::new);
		return checkString("tags", AbstractEntity.VAR_CHAR_TAGS, String.join(",", ss));
	}

	protected String checkUrl(String url) {
		url = checkString("url", AbstractEntity.VAR_CHAR_URL, url);
		if (url.startsWith("https://") || url.startsWith("http://") || url.startsWith("/")) {
			return url;
		}
		throw new ApiException(ApiError.PARAMETER_INVALID, "url", "Invalid URL.");
	}

	private String checkString(String paramName, int maxLength, String s) {
		if (s == null) {
			throw new ApiException(ApiError.PARAMETER_INVALID, paramName,
					"Parameter " + paramName + " must not be null.");
		}
		s = s.strip();
		if (s.isEmpty()) {
			throw new ApiException(ApiError.PARAMETER_INVALID, paramName,
					"Parameter " + paramName + " must not be emtpy.");
		}
		if (s.length() > maxLength) {
			throw new ApiException(ApiError.PARAMETER_INVALID, paramName, "Parameter " + paramName + " is too long.");
		}
		return s;
	}

	private static final Pattern PATTERN_TAG = Pattern.compile("^[^\\,\\;]{1," + AbstractEntity.VAR_ENUM + "}$");
	private static final Pattern PATTERN_HASHED_PASSWORD = Pattern.compile("^[a-f0-9]{64}$");
}
