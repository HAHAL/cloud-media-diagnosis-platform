package com.example.clouddiagnosis.util;

import cn.hutool.core.util.StrUtil;
import com.example.clouddiagnosis.model.common.CacheStatus;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class HeaderUtils {
    public static final List<String> KEY_HTTP_HEADERS = List.of(
            "Server", "Content-Type", "Content-Length", "Cache-Control", "Age", "Via",
            "X-Cache", "X-Cache-Status", "Location", "Accept-Ranges"
    );

    public static final List<String> KEY_CDN_HEADERS = List.of(
            "Cache-Control", "Expires", "Age", "Via", "X-Cache", "X-Cache-Status", "ETag", "Last-Modified", "Set-Cookie"
    );

    private HeaderUtils() {
    }

    public static String getIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null || name == null) {
            return null;
        }
        return headers.entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public static boolean isCdnLike(Map<String, String> headers) {
        String via = getIgnoreCase(headers, "Via");
        String age = getIgnoreCase(headers, "Age");
        String xCache = getIgnoreCase(headers, "X-Cache");
        String xCacheStatus = getIgnoreCase(headers, "X-Cache-Status");
        String server = Optional.ofNullable(getIgnoreCase(headers, "Server")).orElse("");
        String merged = String.join(" ", StrUtil.nullToEmpty(via), StrUtil.nullToEmpty(age), StrUtil.nullToEmpty(xCache),
                StrUtil.nullToEmpty(xCacheStatus), server).toLowerCase(Locale.ROOT);
        return StrUtil.isNotBlank(via) || StrUtil.isNotBlank(age) || merged.contains("cdn")
                || merged.contains("cache") || merged.contains("cloudfront") || merged.contains("akamai");
    }

    public static boolean isRangeSupported(Map<String, String> headers) {
        String acceptRanges = getIgnoreCase(headers, "Accept-Ranges");
        String contentRange = getIgnoreCase(headers, "Content-Range");
        return acceptRanges != null && acceptRanges.toLowerCase(Locale.ROOT).contains("bytes")
                || StrUtil.isNotBlank(contentRange);
    }

    public static CacheStatus judgeCacheStatus(Map<String, String> headers) {
        String xCache = StrUtil.nullToEmpty(getIgnoreCase(headers, "X-Cache")).toLowerCase(Locale.ROOT);
        String xCacheStatus = StrUtil.nullToEmpty(getIgnoreCase(headers, "X-Cache-Status")).toLowerCase(Locale.ROOT);
        String cacheControl = StrUtil.nullToEmpty(getIgnoreCase(headers, "Cache-Control")).toLowerCase(Locale.ROOT);
        String merged = xCache + " " + xCacheStatus;
        if (merged.contains("hit")) {
            return CacheStatus.HIT;
        }
        if (merged.contains("miss")) {
            return CacheStatus.MISS;
        }
        if (merged.contains("bypass") || cacheControl.contains("no-store") || cacheControl.contains("private")) {
            return CacheStatus.BYPASS;
        }
        return CacheStatus.UNKNOWN;
    }

    public static Long parseContentLength(Map<String, String> headers) {
        String value = getIgnoreCase(headers, "Content-Length");
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
