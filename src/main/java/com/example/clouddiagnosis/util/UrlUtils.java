package com.example.clouddiagnosis.util;

import cn.hutool.core.util.StrUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

public final class UrlUtils {
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^(?=.{1,253}$)([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$");

    private UrlUtils() {
    }

    public static boolean isValidHttpUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return false;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && StrUtil.isNotBlank(uri.getHost());
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    public static String extractHost(String url) {
        try {
            return new URI(url).getHost();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    public static boolean isValidDomain(String domain) {
        return StrUtil.isNotBlank(domain) && DOMAIN_PATTERN.matcher(domain).matches();
    }

    public static String detectResourceType(String url, String contentType) {
        String lowerUrl = StrUtil.blankToDefault(url, "").toLowerCase(Locale.ROOT);
        String lowerContentType = StrUtil.blankToDefault(contentType, "").toLowerCase(Locale.ROOT);
        if (lowerUrl.contains(".m3u8") || lowerContentType.contains("mpegurl")) {
            return "m3u8";
        }
        if (lowerUrl.contains(".flv") || lowerContentType.contains("x-flv")) {
            return "flv";
        }
        if (lowerUrl.contains(".mp4") || lowerContentType.contains("mp4")) {
            return "mp4";
        }
        if (lowerContentType.startsWith("video/")) {
            return "video";
        }
        return "unknown";
    }
}
