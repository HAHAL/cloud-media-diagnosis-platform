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

    public FullDiagnoseResponse diagnose(FullDiagnoseRequest request) {
        if (!UrlUtils.isValidHttpUrl(request.getUrl())) {
            throw new BizException("URL 格式不合法");
        }
        String domain = UrlUtils.extractHost(request.getUrl());
        DnsDiagnoseRequest dnsRequest = new DnsDiagnoseRequest();
        dnsRequest.setDomain(domain);
        DnsDiagnoseResponse dns = dnsDiagnoseService.diagnose(dnsRequest);

        HttpDiagnoseRequest httpRequest = new HttpDiagnoseRequest();
        httpRequest.setUrl(request.getUrl());
        httpRequest.setTimeoutMs(5000);
        httpRequest.setFollowRedirect(true);
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
            rootCauseHints.add("HTTP 状态码异常，需区分 CDN 节点拒绝、缓存异常还是源站返回异常");
        }
        if (Boolean.FALSE.equals(http.getCdnLike()) && Boolean.FALSE.equals(dns.getCdnLike())) {
            rootCauseHints.add("未发现明显 CDN 特征，可能未接入 CDN 或响应头被隐藏");
        }
        if (cdnHeader.getCacheStatus().name().matches("MISS|BYPASS|UNKNOWN")) {
            rootCauseHints.add("缓存命中状态不明确或未命中，访问慢可能与回源耗时有关");
        }
        if (video != null && Boolean.FALSE.equals(video.getRangeSupported())) {
            rootCauseHints.add("视频资源未检测到 Range 支持，可能导致拖拽失败或首帧体验差");
        }
        if (rootCauseHints.isEmpty()) {
            rootCauseHints.add("当前基础检测未发现高风险异常，建议结合客户侧地域、运营商和时间点继续定位");
        }
        nextActions.add("按客户访问地域和运营商执行 DNS 解析与 CDN 节点访问对比");
        nextActions.add("检查 CDN 缓存命中率、回源状态码、回源耗时和刷新预热记录");
        nextActions.add("对比 CDN URL 与源站 URL 的状态码、响应头和响应时间");
        nextActions.add("收集客户访问时间点、异常截图、客户端 IP、TraceId 或 CDN 请求 ID");

        String summary = switch (risk) {
            case LOW -> "当前 URL 基础访问正常，未发现明显高风险异常。";
            case MEDIUM -> "当前 URL 存在缓存、解析或视频体验相关的中风险线索。";
            case HIGH -> "当前 URL 存在访问失败、解析失败或服务端异常等高风险问题。";
        };

        return FullDiagnoseResponse.builder()
                .url(request.getUrl())
                .overallRiskLevel(risk)
                .summary(summary)
                .rootCauseHints(rootCauseHints)
                .nextActions(nextActions)
                .customerReplyTemplate(buildCustomerReply(http))
                .dns(dns)
                .http(http)
                .cdnHeader(cdnHeader)
                .video(video)
                .build();
    }

    private String buildCustomerReply(HttpDiagnoseResponse http) {
        String status = http.getStatusCode() == null ? "未能获取到有效 HTTP 状态码" : "HTTP 状态码为 " + http.getStatusCode();
        String cdn = Boolean.TRUE.equals(http.getCdnLike()) ? "疑似已经过 CDN 节点" : "暂未检测到明显 CDN 响应特征";
        return "您好，当前已根据您提供的 URL 完成初步检测。从检测结果看，该资源" + status + "，"
                + cdn + "。当前建议继续确认缓存命中情况及源站响应耗时。如您方便，请补充具体访问慢的地区、运营商、访问时间点和异常截图，我们会进一步结合节点日志协助定位。";
    }
}
