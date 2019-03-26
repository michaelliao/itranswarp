package com.itranswarp.bean;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.model.AbstractEntity;

public abstract class AbstractRequestBean {

	private static final Pattern PATTERN_TAG = Pattern.compile("^[^\\,\\;]{1," + AbstractEntity.VAR_ENUM + "}$");
	private static final Pattern PATTERN_HASH = Pattern.compile("^[a-f0-9]{64}$");

	public abstract void validate(boolean createMode);

	protected String checkPassword(String value) {
		if (value == null || value.length() != AbstractEntity.VAR_CHAR_HASH) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "password", "Invalid password.");
		}
		Matcher matcher = PATTERN_HASH.matcher(value);
		if (!matcher.matches()) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "password", "Invalid password.");
		}
		return value;
	}

	protected void checkId(String name, long value) {
		if (value <= 0) {
			throw new ApiException(ApiError.PARAMETER_INVALID, name, "Invalid id.");
		}
	}

	protected void checkPublishAt(long value) {
		if (value < 0) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "publishAt", "Invalid publishAt.");
		}
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

	protected String checkContent(String value) {
		return checkString("content", 65535, value);
	}

	protected String checkImage(String value) {
		return checkString("image", 524287, value);
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

}
