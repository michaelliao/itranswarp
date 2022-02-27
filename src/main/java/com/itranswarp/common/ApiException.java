package com.itranswarp.common;

import java.util.HashMap;
import java.util.Map;

import com.itranswarp.enums.ApiError;

/**
 * Base exception for API.
 */
public class ApiException extends RuntimeException {

    public final ApiError error;
    public final String data;

    public ApiException(ApiError error) {
        super(error.name());
        this.error = error;
        this.data = null;
    }

    public ApiException(ApiError error, String data, String message) {
        super(message);
        this.error = error;
        this.data = data;
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("error", this.error.name());
        map.put("data", this.data);
        map.put("message", this.getMessage());
        return map;
    }
}
