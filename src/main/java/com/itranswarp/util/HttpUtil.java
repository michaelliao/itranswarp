package com.itranswarp.util;

import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

public class HttpUtil {

    public static String getOrigin(HttpServletRequest request) {
        String scheme = getScheme(request);
        String host = getHost(request);
        return scheme + "://" + host;
    }

    /**
     * Get host as "localhost:8080".
     */
    public static String getHost(HttpServletRequest request) {
        String host = request.getHeader("HOST");
        if (host == null) {
            host = "localhost";
        } else {
            host = host.toLowerCase();
        }
        return host;
    }

    /**
     * Get hostname only as "localhost".
     */
    public static String getHostname(HttpServletRequest request) {
        String host = request.getHeader("HOST");
        if (host == null) {
            host = "localhost";
        } else {
            host = host.toLowerCase();
        }
        int n = host.indexOf(':');
        if (n > 0) {
            host = host.substring(0, n);
        }
        return host;
    }

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
        if (url.startsWith("/auth/") || url.startsWith("/api/")) {
            return "/";
        }
        return url;
    }

    static class DevicePattern {
        final String name;
        final Pattern pattern;

        DevicePattern(String name, String pattern) {
            this.name = name;
            this.pattern = Pattern.compile(pattern);
        }

        boolean matches(String s) {
            return this.pattern.matcher(s).find();
        }
    }

    static DevicePattern[] devicePatterns = new DevicePattern[] { //
            new DevicePattern("Windows", "\\Wwindows\\snt\\W"), //
            new DevicePattern("macOS", "\\Wmacintosh\\W"), //
            new DevicePattern("Android", "\\Wandroid\\W"), //
            new DevicePattern("iPhone", "\\Wiphone\\W"), //
            new DevicePattern("iPad", "\\Wipad\\W"), //
            new DevicePattern("Linux", "\\Wlinux\\W"), };

    public static String getDevice(HttpServletRequest request) {
        String ua = request.getHeader("USER-AGENT");
        if (ua != null) {
            ua = ua.toLowerCase();
            for (var device : devicePatterns) {
                if (device.matches(ua)) {
                    return device.name;
                }
            }
        }
        return "Unknown";
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
