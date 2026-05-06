package com.example.clouddiagnosis.service;

import com.example.clouddiagnosis.model.common.CacheStatus;
import com.example.clouddiagnosis.model.common.RiskLevel;
import com.example.clouddiagnosis.model.request.CdnHeaderAnalyzeRequest;
import com.example.clouddiagnosis.model.request.HttpDiagnoseRequest;
import com.example.clouddiagnosis.model.response.CdnHeaderAnalyzeResponse;
import com.example.clouddiagnosis.model.response.HttpDiagnoseResponse;
import com.example.clouddiagnosis.util.HeaderUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CdnHeaderAnalyzeService {
    private final HttpDiagnoseService httpDiagnoseService;
    private final DiagnosisCacheService cacheService;

    public CdnHeaderAnalyzeResponse analyze(CdnHeaderAnalyzeRequest request) {
        String cacheKey = "diagnosis:header:" + DiagnosisCacheService.hash(request.getUrl());
        if (Boolean.TRUE.equals(request.getUseCache())) {
            var cached = cacheService.get(cacheKey, CdnHeaderAnalyzeResponse.class);
            if (cached.isPresent()) {
                CdnHeaderAnalyzeResponse response = cached.get();
                response.setCached(true);
                response.setCacheKey(cacheKey);
                response.setCacheTtlSeconds(cacheService.ttlSeconds());
                return response;
            }
        }
        HttpDiagnoseRequest httpRequest = new HttpDiagnoseRequest();
        httpRequest.setUrl(request.getUrl());
        httpRequest.setTimeoutMs(5000);
        httpRequest.setFollowRedirect(true);
        httpRequest.setUseCache(request.getUseCache());
        HttpDiagnoseResponse http = httpDiagnoseService.diagnose(httpRequest);
        CdnHeaderAnalyzeResponse result = analyzeFromHttp(http);
        result.setCached(false);
        result.setCacheKey(cacheKey);
        result.setCacheTtlSeconds(cacheService.ttlSeconds());
        if (Boolean.TRUE.equals(request.getUseCache())) {
            cacheService.put(cacheKey, result);
        }
        return result;
    }

    public CdnHeaderAnalyzeResponse analyzeFromHttp(HttpDiagnoseResponse http) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : HeaderUtils.KEY_CDN_HEADERS) {
            String value = HeaderUtils.getIgnoreCase(http.getHeaders(), name);
            if (value != null) {
                headers.put(name, value);
            }
        }
        CacheStatus cacheStatus = HeaderUtils.judgeCacheStatus(http.getHeaders());
        List<String> diagnosis = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        RiskLevel riskLevel = http.getRiskLevel();

        String cacheControl = HeaderUtils.getIgnoreCase(http.getHeaders(), "Cache-Control");
        String xCache = HeaderUtils.getIgnoreCase(http.getHeaders(), "X-Cache");
        String age = HeaderUtils.getIgnoreCase(http.getHeaders(), "Age");
        String setCookie = HeaderUtils.getIgnoreCase(http.getHeaders(), "Set-Cookie");

        if (cacheControl != null && cacheControl.toLowerCase(Locale.ROOT).matches(".*(no-cache|no-store|private).*")) {
            diagnosis.add("Cache-Control 包含 no-cache/no-store/private，响应可能不会被浏览器、网关或缓存层复用");
            suggestions.add("检查应用响应头、网关缓存规则和客户端缓存策略是否符合预期");
            riskLevel = RiskLevel.MEDIUM;
        }
        if (age == null) {
            diagnosis.add("未发现 Age Header，可能未命中缓存、直连应用服务或中间代理未透出缓存年龄");
            suggestions.add("结合 X-Cache、Via、网关日志和应用日志确认是否命中缓存层");
        }
        if (xCache != null && xCache.toLowerCase(Locale.ROOT).contains("hit")) {
            diagnosis.add("X-Cache 显示 HIT，响应疑似命中缓存");
        } else if (xCache != null && xCache.toLowerCase(Locale.ROOT).contains("miss")) {
            diagnosis.add("X-Cache 显示 MISS，当前请求疑似穿透缓存访问上游服务");
            suggestions.add("检查缓存规则、URL 参数、刷新预热记录和应用响应缓存头");
            riskLevel = RiskLevel.MEDIUM;
        }
        if (setCookie != null) {
            diagnosis.add("响应包含 Set-Cookie，缓存层通常会将其视为动态响应");
            suggestions.add("确认该 URL 是否应缓存，必要时调整缓存键、Cookie 忽略策略或动态接口缓存策略");
        }
        suggestions.add("检查网关缓存配置、URL 是否带动态参数、应用是否返回可缓存响应头，以及 CORS/安全 Header 是否满足调用方要求");

        return CdnHeaderAnalyzeResponse.builder()
                .url(http.getUrl())
                .statusCode(http.getStatusCode())
                .cacheStatus(cacheStatus)
                .riskLevel(riskLevel)
                .headers(headers)
                .diagnosis(diagnosis)
                .suggestions(suggestions)
                .build();
    }
}
