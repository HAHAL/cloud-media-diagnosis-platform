package com.example.clouddiagnosis.service;

import com.example.clouddiagnosis.model.common.RiskLevel;
import com.example.clouddiagnosis.model.request.HttpDiagnoseRequest;
import com.example.clouddiagnosis.model.request.OriginCompareRequest;
import com.example.clouddiagnosis.model.response.HttpDiagnoseResponse;
import com.example.clouddiagnosis.model.response.OriginCompareResponse;
import com.example.clouddiagnosis.util.DiagnoseRuleUtils;
import com.example.clouddiagnosis.util.HeaderUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OriginCompareService {
    private final HttpDiagnoseService httpDiagnoseService;

    public OriginCompareResponse compare(OriginCompareRequest request) {
        HttpDiagnoseRequest cdnRequest = new HttpDiagnoseRequest();
        cdnRequest.setUrl(request.getCdnUrl());
        cdnRequest.setTimeoutMs(5000);
        cdnRequest.setFollowRedirect(true);

        HttpDiagnoseRequest originRequest = new HttpDiagnoseRequest();
        originRequest.setUrl(request.getOriginUrl());
        originRequest.setTimeoutMs(5000);
        originRequest.setFollowRedirect(true);

        HttpDiagnoseResponse cdn = httpDiagnoseService.diagnose(cdnRequest);
        HttpDiagnoseResponse origin = httpDiagnoseService.diagnose(originRequest, request.getHostHeader());

        List<String> diagnosis = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        boolean cdnOk = is2xx(cdn.getStatusCode());
        boolean originOk = is2xx(origin.getStatusCode());

        if (cdnOk && !originOk) {
            diagnosis.add("CDN 访问正常、源站访问异常，疑似 CDN 缓存仍可服务但源站存在问题");
            suggestions.add("优先检查源站服务、源站负载、回源端口和健康检查状态");
        }
        if (!cdnOk && originOk) {
            diagnosis.add("CDN 访问异常、源站访问正常，疑似 CDN 配置、缓存或回源策略异常");
            suggestions.add("检查 CDN 回源 Host、缓存状态码、访问控制、防盗链和节点日志");
        }
        if (cdn.getResponseTimeMs() != null && origin.getResponseTimeMs() != null) {
            long diff = cdn.getResponseTimeMs() - origin.getResponseTimeMs();
            if (diff > 500) {
                diagnosis.add("CDN 响应明显慢于源站，可能存在节点调度异常、缓存未命中或节点到源站链路慢");
            } else if (diff < -500) {
                diagnosis.add("CDN 响应明显快于源站，说明 CDN 缓存或边缘节点加速效果较好");
            }
        }
        if (!Objects.equals(cdn.getStatusCode(), origin.getStatusCode())) {
            diagnosis.add("CDN 与源站状态码不一致");
            suggestions.add("检查缓存、回源 Host、源站配置、重定向规则和是否缓存了历史异常响应");
        }
        compareHeader("Content-Length", cdn, origin, diagnosis);
        compareHeader("Content-Type", cdn, origin, diagnosis);
        compareHeader("Server", cdn, origin, diagnosis);
        if (diagnosis.isEmpty()) {
            diagnosis.add("CDN 与源站核心响应信息基本一致，建议继续结合地域、运营商和日志定位慢请求");
        }

        return OriginCompareResponse.builder()
                .cdnResult(cdn)
                .originResult(origin)
                .riskLevel(DiagnoseRuleUtils.max(cdn.getRiskLevel(), origin.getRiskLevel()))
                .diagnosis(diagnosis)
                .suggestions(suggestions)
                .build();
    }

    private boolean is2xx(Integer statusCode) {
        return statusCode != null && statusCode >= 200 && statusCode < 300;
    }

    private void compareHeader(String header, HttpDiagnoseResponse cdn, HttpDiagnoseResponse origin, List<String> diagnosis) {
        String cdnValue = HeaderUtils.getIgnoreCase(cdn.getHeaders(), header);
        String originValue = HeaderUtils.getIgnoreCase(origin.getHeaders(), header);
        if (cdnValue != null && originValue != null && !cdnValue.equals(originValue)) {
            diagnosis.add(header + " 不一致：CDN=" + cdnValue + "，源站=" + originValue);
        }
    }
}
