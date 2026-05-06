package com.example.clouddiagnosis.service;

import com.example.clouddiagnosis.exception.BizException;
import com.example.clouddiagnosis.model.common.RiskLevel;
import com.example.clouddiagnosis.model.request.CdnHeaderAnalyzeRequest;
import com.example.clouddiagnosis.model.request.DnsDiagnoseRequest;
import com.example.clouddiagnosis.model.request.FullDiagnoseRequest;
import com.example.clouddiagnosis.model.request.HttpDiagnoseRequest;
import com.example.clouddiagnosis.model.response.CdnHeaderAnalyzeResponse;
import com.example.clouddiagnosis.model.response.DnsDiagnoseResponse;
import com.example.clouddiagnosis.model.response.FullDiagnoseResponse;
import com.example.clouddiagnosis.model.response.HttpDiagnoseResponse;
import com.example.clouddiagnosis.model.response.VideoDiagnoseResponse;
import com.example.clouddiagnosis.util.DiagnoseRuleUtils;
import com.example.clouddiagnosis.util.UrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FullDiagnoseService {
    private final DnsDiagnoseService dnsDiagnoseService;
    private final HttpDiagnoseService httpDiagnoseService;
    private final CdnHeaderAnalyzeService cdnHeaderAnalyzeService;
    private final VideoDiagnoseService videoDiagnoseService;
    private final DiagnosisCacheService cacheService;
    private final DiagnosisRecordService diagnosisRecordService;

    public FullDiagnoseResponse diagnose(FullDiagnoseRequest request) {
        if (!UrlUtils.isValidHttpUrl(request.getUrl())) {
            throw new BizException("URL 格式不合法");
        }
        String cacheKey = "diagnosis:full:" + DiagnosisCacheService.hash(request.getUrl() + "|" + request.getScenario());
        if (Boolean.TRUE.equals(request.getUseCache())) {
            var cached = cacheService.get(cacheKey, FullDiagnoseResponse.class);
            if (cached.isPresent()) {
                FullDiagnoseResponse response = cached.get();
                response.setCached(true);
                response.setCacheKey(cacheKey);
                response.setCacheTtlSeconds(cacheService.ttlSeconds());
                if (Boolean.TRUE.equals(request.getSaveRecord())) {
                    response.setRecordId(diagnosisRecordService.saveFullDiagnosis(response));
                }
                return response;
            }
        }
        String domain = UrlUtils.extractHost(request.getUrl());
        DnsDiagnoseRequest dnsRequest = new DnsDiagnoseRequest();
        dnsRequest.setDomain(domain);
        dnsRequest.setUseCache(request.getUseCache());
        DnsDiagnoseResponse dns = dnsDiagnoseService.diagnose(dnsRequest);

        HttpDiagnoseRequest httpRequest = new HttpDiagnoseRequest();
        httpRequest.setUrl(request.getUrl());
        httpRequest.setTimeoutMs(5000);
        httpRequest.setFollowRedirect(true);
        httpRequest.setUseCache(request.getUseCache());
        HttpDiagnoseResponse http = httpDiagnoseService.diagnose(httpRequest);

        CdnHeaderAnalyzeResponse cdnHeader = cdnHeaderAnalyzeService.analyzeFromHttp(http);
        boolean videoResource = !"unknown".equals(UrlUtils.detectResourceType(request.getUrl(), http.getHeaders().get("Content-Type")));
        VideoDiagnoseResponse video = videoResource ? videoDiagnoseService.diagnoseFromHttp(http) : null;

        RiskLevel risk = video == null
                ? DiagnoseRuleUtils.max(dns.getRiskLevel(), http.getRiskLevel(), cdnHeader.getRiskLevel())
                : DiagnoseRuleUtils.max(dns.getRiskLevel(), http.getRiskLevel(), cdnHeader.getRiskLevel(), video.getRiskLevel());

        List<String> rootCauseHints = new ArrayList<>();
        List<String> nextActions = new ArrayList<>();
        if (dns.getIps().isEmpty()) {
            rootCauseHints.add("域名无 A 记录解析结果，可能是 DNS 配置或递归解析异常");
        }
        if (http.getStatusCode() != null && http.getStatusCode() >= 400) {
            rootCauseHints.add("HTTP 状态码异常，需区分网关、缓存层、应用服务还是上游依赖返回异常");
        }
        if (Boolean.FALSE.equals(http.getCdnLike()) && Boolean.FALSE.equals(dns.getCdnLike())) {
            rootCauseHints.add("未发现明显代理或缓存特征，可能直连应用服务或响应头被中间层隐藏");
        }
        if (cdnHeader.getCacheStatus().name().matches("MISS|BYPASS|UNKNOWN")) {
            rootCauseHints.add("缓存命中状态不明确或未命中，访问慢可能与上游服务响应耗时有关");
        }
        if (video != null && Boolean.FALSE.equals(video.getRangeSupported())) {
            rootCauseHints.add("媒体或大文件资源未检测到 Range 支持，可能导致拖拽、续传或分片加载异常");
        }
        if (rootCauseHints.isEmpty()) {
            rootCauseHints.add("当前基础检测未发现高风险异常，建议结合访问地域、网络出口、请求时间和应用日志继续定位");
        }
        nextActions.add("按访问地域和网络出口执行 DNS 解析与 URL/API 连通性对比");
        nextActions.add("检查网关 access log、upstream_response_time、应用日志和上游依赖耗时");
        nextActions.add("对比公网入口 URL 与上游服务 URL 的状态码、响应头和响应时间");
        nextActions.add("收集请求时间、客户端 IP、requestId/traceId、影响范围和完整错误响应");

        String summary = switch (risk) {
            case LOW -> "当前 URL/API 基础访问正常，未发现明显高风险异常。";
            case MEDIUM -> "当前 URL/API 存在缓存、解析、响应头或访问体验相关的中风险线索。";
            case HIGH -> "当前 URL/API 存在访问失败、解析失败、网关或应用服务异常等高风险问题。";
        };

        FullDiagnoseResponse result = FullDiagnoseResponse.builder()
                .url(request.getUrl())
                .overallRiskLevel(risk)
                .summary(summary)
                .rootCauseHints(rootCauseHints)
                .nextActions(nextActions)
                .customerReplyTemplate(buildCustomerReply(http))
                .cached(false)
                .cacheKey(cacheKey)
                .cacheTtlSeconds(cacheService.ttlSeconds())
                .dns(dns)
                .http(http)
                .cdnHeader(cdnHeader)
                .video(video)
                .build();
        if (Boolean.TRUE.equals(request.getUseCache())) {
            cacheService.put(cacheKey, result);
        }
        if (Boolean.TRUE.equals(request.getSaveRecord())) {
            result.setRecordId(diagnosisRecordService.saveFullDiagnosis(result));
        }
        return result;
    }

    private String buildCustomerReply(HttpDiagnoseResponse http) {
        String status = http.getStatusCode() == null ? "未能获取到有效 HTTP 状态码" : "HTTP 状态码为 " + http.getStatusCode();
        String cdn = Boolean.TRUE.equals(http.getCdnLike()) ? "疑似已经过网关、代理或缓存层" : "暂未检测到明显代理或缓存响应特征";
        return "您好，当前已根据您提供的 URL 完成初步检测。从检测结果看，该资源" + status + "，"
                + cdn + "。当前建议继续确认缓存命中情况、网关耗时及上游服务响应耗时。如您方便，请补充具体访问时间点、客户端网络环境、requestId/traceId、异常截图和完整错误响应，我们会进一步结合日志协助定位。";
    }
}
