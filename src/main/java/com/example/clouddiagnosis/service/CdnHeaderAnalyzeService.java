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

    public CdnHeaderAnalyzeResponse analyze(CdnHeaderAnalyzeRequest request) {
        HttpDiagnoseRequest httpRequest = new HttpDiagnoseRequest();
        httpRequest.setUrl(request.getUrl());
        httpRequest.setTimeoutMs(5000);
        httpRequest.setFollowRedirect(true);
        HttpDiagnoseResponse http = httpDiagnoseService.diagnose(httpRequest);
        return analyzeFromHttp(http);
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
            diagnosis.add("Cache-Control 包含 no-cache/no-store/private，资源可能不会被 CDN 缓存");
            suggestions.add("检查源站 Cache-Control 与 CDN 控制台缓存规则是否冲突");
            riskLevel = RiskLevel.MEDIUM;
        }
        if (age == null) {
            diagnosis.add("未发现 Age Header，可能未命中缓存、直连源站或 CDN 未透出缓存年龄");
            suggestions.add("结合 X-Cache、Via 和 CDN 日志确认是否命中节点缓存");
        }
        if (xCache != null && xCache.toLowerCase(Locale.ROOT).contains("hit")) {
            diagnosis.add("X-Cache 显示 HIT，资源疑似命中 CDN 缓存");
        } else if (xCache != null && xCache.toLowerCase(Locale.ROOT).contains("miss")) {
            diagnosis.add("X-Cache 显示 MISS，当前请求疑似回源");
            suggestions.add("检查缓存规则、URL 参数、刷新预热记录和源站缓存头");
            riskLevel = RiskLevel.MEDIUM;
        }
        if (setCookie != null) {
            diagnosis.add("响应包含 Set-Cookie，部分 CDN 默认不会缓存带 Cookie 的动态响应");
            suggestions.add("确认该 URL 是否应作为静态资源缓存，必要时调整缓存键和 Cookie 忽略策略");
        }
        suggestions.add("检查 CDN 控制台缓存配置、URL 是否带动态参数、源站是否返回可缓存响应头");

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
