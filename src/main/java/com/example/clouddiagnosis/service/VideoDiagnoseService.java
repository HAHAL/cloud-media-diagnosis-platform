package com.example.clouddiagnosis.service;

import com.example.clouddiagnosis.model.common.RiskLevel;
import com.example.clouddiagnosis.model.request.HttpDiagnoseRequest;
import com.example.clouddiagnosis.model.request.VideoDiagnoseRequest;
import com.example.clouddiagnosis.model.response.HttpDiagnoseResponse;
import com.example.clouddiagnosis.model.response.VideoDiagnoseResponse;
import com.example.clouddiagnosis.util.HeaderUtils;
import com.example.clouddiagnosis.util.UrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VideoDiagnoseService {
    private static final long LARGE_FILE_THRESHOLD = 100L * 1024 * 1024;
    private final HttpDiagnoseService httpDiagnoseService;

    public VideoDiagnoseResponse diagnose(VideoDiagnoseRequest request) {
        HttpDiagnoseRequest httpRequest = new HttpDiagnoseRequest();
        httpRequest.setUrl(request.getUrl());
        httpRequest.setTimeoutMs(5000);
        httpRequest.setFollowRedirect(true);
        return diagnoseFromHttp(httpDiagnoseService.diagnose(httpRequest));
    }

    public VideoDiagnoseResponse diagnoseFromHttp(HttpDiagnoseResponse http) {
        String contentType = HeaderUtils.getIgnoreCase(http.getHeaders(), "Content-Type");
        String videoType = UrlUtils.detectResourceType(http.getUrl(), contentType);
        boolean videoContentType = contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("video/");
        boolean rangeSupported = Boolean.TRUE.equals(http.getRangeSupported());
        Long contentLength = HeaderUtils.parseContentLength(http.getHeaders());
        boolean contentLengthExists = contentLength != null;
        boolean maybeLarge = contentLength != null && contentLength > LARGE_FILE_THRESHOLD;
        List<String> diagnosis = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        RiskLevel riskLevel = http.getRiskLevel();

        if (videoContentType || !"unknown".equals(videoType)) {
            diagnosis.add("资源疑似视频类型：" + videoType);
        } else {
            diagnosis.add("Content-Type 未明确标识为视频类型，可能影响播放器策略或浏览器兼容性");
            suggestions.add("检查源站 MIME 类型配置，确保 mp4/flv/m3u8 返回正确 Content-Type");
            riskLevel = RiskLevel.MEDIUM;
        }
        if (!rangeSupported) {
            diagnosis.add("未检测到 Range 支持，视频拖拽、分片加载或大文件续传可能失败");
            suggestions.add("开启源站 Accept-Ranges 与 CDN Range 回源能力，确认 206 分片响应正常");
            riskLevel = RiskLevel.MEDIUM;
        }
        if (!contentLengthExists) {
            diagnosis.add("未返回 Content-Length，播放器可能无法准确预估缓冲和文件大小");
            suggestions.add("检查源站是否使用 chunked 响应，确认 CDN 是否透传 Content-Length");
        }
        if (maybeLarge) {
            diagnosis.add("文件体积较大，弱网环境下容易出现首帧慢或卡顿");
            suggestions.add("建议使用分片格式、码率自适应、预热热点资源并检查节点缓存命中率");
        }

        switch (videoType) {
            case "mp4" -> suggestions.add("mp4 请检查 moov atom 是否前置，并确认 Range 请求、缓存预热和首包时间");
            case "m3u8" -> suggestions.add("m3u8 请检查 ts 分片访问、跨域 CORS、分片缓存策略和 404 分片丢失问题");
            case "flv" -> suggestions.add("flv 请关注长连接稳定性、首包时间、CDN 节点质量和源站推流链路");
            default -> suggestions.add("结合播放器错误码排查首帧慢、卡顿、拖拽失败、403 防盗链、404 分片丢失和 5xx 源站异常");
        }

        return VideoDiagnoseResponse.builder()
                .url(http.getUrl())
                .videoType(videoType)
                .videoContentType(videoContentType)
                .rangeSupported(rangeSupported)
                .contentLengthExists(contentLengthExists)
                .contentLength(contentLength)
                .maybeLargeFile(maybeLarge)
                .riskLevel(riskLevel)
                .headers(http.getHeaders())
                .diagnosis(diagnosis)
                .suggestions(suggestions)
                .build();
    }
}
