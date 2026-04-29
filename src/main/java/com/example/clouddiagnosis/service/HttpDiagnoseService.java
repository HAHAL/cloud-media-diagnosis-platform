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

    public HttpDiagnoseResponse diagnose(HttpDiagnoseRequest request) {
        return diagnose(request, null);
    }

    public HttpDiagnoseResponse diagnose(HttpDiagnoseRequest request, String hostHeader) {
        if (!UrlUtils.isValidHttpUrl(request.getUrl())) {
            throw new BizException("URL 格式不合法");
        }
        int timeoutMs = request.getTimeoutMs() == null ? 5000 : Math.min(request.getTimeoutMs(), 15000);
        List<String> diagnosis = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        long start = System.nanoTime();
        try {
            HttpResponse<Void> response = send(request.getUrl(), "HEAD", timeoutMs, Boolean.TRUE.equals(request.getFollowRedirect()), hostHeader);
            // 真实排障中部分源站不支持 HEAD，会返回 405/403 或直接断开，这里降级 GET 提高诊断成功率。
            if (response.statusCode() == 405 || response.statusCode() == 501) {
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
                diagnosis.add("检测到 CDN 相关响应头，疑似经过 CDN 节点");
                suggestions.add("可继续对比不同地区、不同运营商的节点解析和缓存命中情况");
            } else {
                diagnosis.add("未检测到明显 CDN Header，可能直连源站或 CDN Header 被隐藏");
                suggestions.add("如业务已接入 CDN，请检查 CNAME、加速域名配置和响应头透传策略");
            }
            if (rangeSupported) {
                diagnosis.add("资源支持 Range 请求，适合视频拖拽和分片加载");
            } else {
                suggestions.add("如为大文件或视频资源，建议源站和 CDN 开启 Range 回源与分片响应能力");
            }

            return HttpDiagnoseResponse.builder()
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
                    .build();
        } catch (Exception headEx) {
            try {
                HttpResponse<Void> response = send(request.getUrl(), "GET", timeoutMs, Boolean.TRUE.equals(request.getFollowRedirect()), hostHeader);
                long cost = (System.nanoTime() - start) / 1_000_000;
                Map<String, String> headers = filterHeaders(response.headers().map(), HeaderUtils.KEY_HTTP_HEADERS);
                DiagnoseRuleUtils.appendStatusDiagnosis(response.statusCode(), diagnosis, suggestions);
                diagnosis.add("HEAD 请求失败，已自动降级为 GET 请求完成诊断");
                return HttpDiagnoseResponse.builder()
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
                        .build();
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
                        .build();
            }
        }
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
