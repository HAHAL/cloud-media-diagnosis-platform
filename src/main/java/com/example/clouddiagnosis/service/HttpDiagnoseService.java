package com.example.clouddiagnosis.service;

import com.example.clouddiagnosis.exception.BizException;
import com.example.clouddiagnosis.model.common.RiskLevel;
import com.example.clouddiagnosis.model.request.HttpDiagnoseRequest;
import com.example.clouddiagnosis.model.response.HttpDiagnoseResponse;
import com.example.clouddiagnosis.util.DiagnoseRuleUtils;
import com.example.clouddiagnosis.util.HeaderUtils;
import com.example.clouddiagnosis.util.UrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class HttpDiagnoseService {
    private final HttpClient httpClient;
    private final DiagnosisCacheService cacheService;

    public HttpDiagnoseResponse diagnose(HttpDiagnoseRequest request) {
        return diagnose(request, null);
    }

    public HttpDiagnoseResponse diagnose(HttpDiagnoseRequest request, String hostHeader) {
        if (!UrlUtils.isValidHttpUrl(request.getUrl())) {
            throw new BizException("URL 格式不合法");
        }
        String method = normalizeMethod(request.getMethod());
        String cacheKey = "diagnosis:http:" + DiagnosisCacheService.hash(method + "|" + request.getUrl() + "|" + request.getTimeoutMs() + "|" + request.getFollowRedirect());
        if (hostHeader == null && Boolean.TRUE.equals(request.getUseCache())) {
            var cached = cacheService.get(cacheKey, HttpDiagnoseResponse.class);
            if (cached.isPresent()) {
                HttpDiagnoseResponse response = cached.get();
                response.setCached(true);
                response.setCacheKey(cacheKey);
                response.setCacheTtlSeconds(cacheService.ttlSeconds());
                return response;
            }
        }
        int timeoutMs = request.getTimeoutMs() == null ? 5000 : Math.min(request.getTimeoutMs(), 15000);
        List<String> diagnosis = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        long start = System.nanoTime();
        try {
            HttpResponse<Void> response = send(request.getUrl(), method, timeoutMs, Boolean.TRUE.equals(request.getFollowRedirect()), hostHeader);
            // 真实排障中部分上游服务不支持 HEAD，会返回 405/501 或直接断开，这里降级 GET 提高诊断成功率。
            if ("HEAD".equals(method) && (response.statusCode() == 405 || response.statusCode() == 501)) {
                response = send(request.getUrl(), "GET", timeoutMs, Boolean.TRUE.equals(request.getFollowRedirect()), hostHeader);
            }
            long cost = (System.nanoTime() - start) / 1_000_000;
            Map<String, String> headers = filterHeaders(response.headers().map(), HeaderUtils.KEY_HTTP_HEADERS);
            boolean redirect = response.statusCode() >= 300 && response.statusCode() < 400 || HeaderUtils.getIgnoreCase(headers, "Location") != null;
            boolean cdnLike = HeaderUtils.isCdnLike(headers);
            boolean rangeSupported = HeaderUtils.isRangeSupported(headers);
            RiskLevel riskLevel = DiagnoseRuleUtils.riskByStatus(response.statusCode());

            DiagnoseRuleUtils.appendStatusDiagnosis(response.statusCode(), diagnosis, suggestions);
            if (redirect) {
                diagnosis.add("检测到重定向信息，需关注 Location 目标地址和跳转次数");
            }
            if (cdnLike) {
                diagnosis.add("检测到代理、缓存或 CDN 相关响应头，疑似经过中间网关或边缘缓存");
                suggestions.add("可继续对比不同地域、不同网络出口下的解析结果、缓存命中和上游响应耗时");
            } else {
                diagnosis.add("未检测到明显代理或缓存 Header，可能直连应用服务或响应头被网关隐藏");
                suggestions.add("如业务经过网关、负载均衡或 CDN，请检查转发链路、Header 透传和缓存策略");
            }
            if (rangeSupported) {
                diagnosis.add("资源支持 Range 请求，适合大文件断点续传、媒体拖拽和分片加载");
            } else {
                suggestions.add("如为大文件或媒体资源，建议确认应用服务、网关和缓存层均支持 Range 响应");
            }

            HttpDiagnoseResponse result = HttpDiagnoseResponse.builder()
                    .url(request.getUrl())
                    .statusCode(response.statusCode())
                    .responseTimeMs(cost)
                    .redirect(redirect)
                    .cdnLike(cdnLike)
                    .rangeSupported(rangeSupported)
                    .headers(headers)
                    .riskLevel(riskLevel)
                    .diagnosis(diagnosis)
                    .suggestions(suggestions)
                    .cached(false)
                    .cacheKey(cacheKey)
                    .cacheTtlSeconds(cacheService.ttlSeconds())
                    .build();
            if (hostHeader == null && Boolean.TRUE.equals(request.getUseCache())) {
                cacheService.put(cacheKey, result);
            }
            return result;
        } catch (Exception headEx) {
            try {
                HttpResponse<Void> response = send(request.getUrl(), "GET", timeoutMs, Boolean.TRUE.equals(request.getFollowRedirect()), hostHeader);
                long cost = (System.nanoTime() - start) / 1_000_000;
                Map<String, String> headers = filterHeaders(response.headers().map(), HeaderUtils.KEY_HTTP_HEADERS);
                DiagnoseRuleUtils.appendStatusDiagnosis(response.statusCode(), diagnosis, suggestions);
                diagnosis.add("HEAD 请求失败，已自动降级为 GET 请求完成诊断");
                HttpDiagnoseResponse result = HttpDiagnoseResponse.builder()
                        .url(request.getUrl())
                        .statusCode(response.statusCode())
                        .responseTimeMs(cost)
                        .redirect(response.statusCode() >= 300 && response.statusCode() < 400)
                        .cdnLike(HeaderUtils.isCdnLike(headers))
                        .rangeSupported(HeaderUtils.isRangeSupported(headers))
                        .headers(headers)
                        .riskLevel(DiagnoseRuleUtils.riskByStatus(response.statusCode()))
                        .diagnosis(diagnosis)
                        .suggestions(suggestions)
                        .cached(false)
                        .cacheKey(cacheKey)
                        .cacheTtlSeconds(cacheService.ttlSeconds())
                        .build();
                if (hostHeader == null && Boolean.TRUE.equals(request.getUseCache())) {
                    cacheService.put(cacheKey, result);
                }
                return result;
            } catch (Exception getEx) {
                diagnosis.add("HTTP 请求失败：" + getEx.getMessage());
                suggestions.add("检查目标站点连通性、TLS 证书、DNS 解析和本地网络出口");
                return HttpDiagnoseResponse.builder()
                        .url(request.getUrl())
                        .responseTimeMs((System.nanoTime() - start) / 1_000_000)
                        .redirect(false)
                        .cdnLike(false)
                        .rangeSupported(false)
                        .headers(Map.of())
                        .riskLevel(RiskLevel.HIGH)
                        .diagnosis(diagnosis)
                        .suggestions(suggestions)
                        .errorMessage(getEx.getMessage())
                        .cached(false)
                        .cacheKey(cacheKey)
                        .cacheTtlSeconds(cacheService.ttlSeconds())
                        .build();
            }
        }
    }

    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) {
            return "GET";
        }
        String upper = method.trim().toUpperCase(Locale.ROOT);
        return List.of("GET", "HEAD", "POST", "PUT", "DELETE", "PATCH", "OPTIONS").contains(upper) ? upper : "GET";
    }

    private HttpResponse<Void> send(String url, String method, int timeoutMs, boolean followRedirect, String hostHeader) throws Exception {
        HttpClient client = followRedirect
                ? HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofMillis(timeoutMs)).build()
                : httpClient;
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .header("User-Agent", "cloud-media-diagnosis-platform/1.0");
        if (hostHeader != null && !hostHeader.isBlank()) {
            builder.header("Host", hostHeader);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.discarding());
    }

    private Map<String, String> filterHeaders(Map<String, List<String>> source, List<String> names) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String name : names) {
            source.entrySet().stream()
                    .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                    .findFirst()
                    .ifPresent(entry -> result.put(name, String.join(",", entry.getValue())));
        }
        source.forEach((key, value) -> {
            String lower = key.toLowerCase(Locale.ROOT);
            if ((lower.startsWith("x-") || "etag".equals(lower) || "last-modified".equals(lower) || "expires".equals(lower) || "set-cookie".equals(lower))
                    && !result.containsKey(key)) {
                result.put(key, String.join(",", value));
            }
        });
        return result;
    }
}
