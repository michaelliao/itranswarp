package com.itranswarp.util;

import jakarta.servlet.http.HttpServletRequest;

public class HttpUtil {

    public static String getScheme(HttpServletRequest request) {
        if ("https".equals(request.getHeader("X-FORWARDED-PROTO"))) {
            return "https";
        }
        return request.getScheme();
    }

    public static String getReferer(HttpServletRequest request) {
        String url = request.getHeader("REFERER");
        if (url == null) {
            return "/";
        }
        if (url.startsWith("https://") || url.startsWith("http://")) {
            int n = url.indexOf('/', 8);
            if (n == (-1)) {
                return "/";
            }
            url = url.substring(n);
        }
        if (url.startsWith("/auth/")) {
            return "/";
        }
        return url;
    }

    public static boolean isSecure(HttpServletRequest request) {
        return "https".equals(getScheme(request));
    }

    /**
     * Try parse location from IP.
     *
     * @param request The http request.
     * @return Location like "US".
     */
    public static String getIPLocation(HttpServletRequest request) {
        String location = null;
        location = request.getHeader("CF-IPCOUNTRY");
        if (location != null && !location.isEmpty()) {
            return location;
        }
        return "UNKNOWN";
    }

    /**
     * Try get IP address from request.
     *
     * @param request The http request.
     * @return IPv4 address like "10.0.1.1".
     */
    public static String getIPAddress(HttpServletRequest request) {
        String ip = null;
        ip = request.getHeader("CF-CONNECTING-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        ip = request.getHeader("X-REAL-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        ip = request.getHeader("X-FORWARDED-FOR");
        if (ip != null && !ip.isEmpty()) {
            int pos = ip.indexOf(',');
            if (pos == -1) {
                return ip;
            }
            return ip.substring(0, pos);
        }
        ip = request.getRemoteAddr();
        // convert IPv6 to IPv4:
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    /**
     * Try get user-agent from request.
     *
     * @param request The http request.
     * @return The user agent like "Mozilla/5.0 (Macintosh) Chrome/71.0"
     */
    public static String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("USER-AGENT");
        if (ua == null) {
            return "";
        }
        if (ua.length() > 997) {
            ua = ua.substring(0, 997) + "...";
        }
        return ua;
    }

}
