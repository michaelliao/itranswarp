package com.itranswarp.common;

import java.util.Map;

import com.itranswarp.enums.ApiError;

/**
 * Base exception for API.
 */
public class ApiException extends RuntimeException {

	public final String error;
	public final String data;

	public ApiException(ApiError error) {
		super(error.name());
		this.error = error.name();
		this.data = null;
	}

	public ApiException(ApiError error, String data, String message) {
		super(message);
		this.error = error.name();
		this.data = data;
	}

	public Map<String, String> toMap() {
		return Map.of("error", this.error, "data", this.data, "message", this.getMessage());
	}
}
