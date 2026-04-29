package com.example.clouddiagnosis.controller;

import com.example.clouddiagnosis.model.common.ApiResponse;
import com.example.clouddiagnosis.model.request.CdnHeaderAnalyzeRequest;
import com.example.clouddiagnosis.model.request.DnsDiagnoseRequest;
import com.example.clouddiagnosis.model.request.FullDiagnoseRequest;
import com.example.clouddiagnosis.model.request.HttpDiagnoseRequest;
import com.example.clouddiagnosis.model.request.OriginCompareRequest;
import com.example.clouddiagnosis.model.request.VideoDiagnoseRequest;
import com.example.clouddiagnosis.model.response.CdnHeaderAnalyzeResponse;
import com.example.clouddiagnosis.model.response.DnsDiagnoseResponse;
import com.example.clouddiagnosis.model.response.FullDiagnoseResponse;
import com.example.clouddiagnosis.model.response.HttpDiagnoseResponse;
import com.example.clouddiagnosis.model.response.OriginCompareResponse;
import com.example.clouddiagnosis.model.response.VideoDiagnoseResponse;
import com.example.clouddiagnosis.service.CdnHeaderAnalyzeService;
import com.example.clouddiagnosis.service.DnsDiagnoseService;
import com.example.clouddiagnosis.service.FullDiagnoseService;
import com.example.clouddiagnosis.service.HttpDiagnoseService;
import com.example.clouddiagnosis.service.OriginCompareService;
import com.example.clouddiagnosis.service.VideoDiagnoseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "诊断接口", description = "HTTP、DNS、CDN Header、源站对比、视频资源和综合诊断")
@RestController
@RequestMapping("/api/diagnose")
@RequiredArgsConstructor
public class DiagnoseController {
    private final HttpDiagnoseService httpDiagnoseService;
    private final DnsDiagnoseService dnsDiagnoseService;
    private final CdnHeaderAnalyzeService cdnHeaderAnalyzeService;
    private final OriginCompareService originCompareService;
    private final VideoDiagnoseService videoDiagnoseService;
    private final FullDiagnoseService fullDiagnoseService;

    @Operation(summary = "HTTP URL 诊断")
    @PostMapping("/http")
    public ApiResponse<HttpDiagnoseResponse> http(@Valid @RequestBody HttpDiagnoseRequest request) {
        return ApiResponse.success(httpDiagnoseService.diagnose(request));
    }

    @Operation(summary = "DNS 诊断")
    @PostMapping("/dns")
    public ApiResponse<DnsDiagnoseResponse> dns(@Valid @RequestBody DnsDiagnoseRequest request) {
        return ApiResponse.success(dnsDiagnoseService.diagnose(request));
    }

    @Operation(summary = "CDN Header 分析")
    @PostMapping("/cdn-header")
    public ApiResponse<CdnHeaderAnalyzeResponse> cdnHeader(@Valid @RequestBody CdnHeaderAnalyzeRequest request) {
        return ApiResponse.success(cdnHeaderAnalyzeService.analyze(request));
    }

    @Operation(summary = "源站对比诊断")
    @PostMapping("/origin-compare")
    public ApiResponse<OriginCompareResponse> originCompare(@Valid @RequestBody OriginCompareRequest request) {
        return ApiResponse.success(originCompareService.compare(request));
    }

    @Operation(summary = "视频资源诊断")
    @PostMapping("/video")
    public ApiResponse<VideoDiagnoseResponse> video(@Valid @RequestBody VideoDiagnoseRequest request) {
        return ApiResponse.success(videoDiagnoseService.diagnose(request));
    }

    @Operation(summary = "综合诊断")
    @PostMapping("/full")
    public ApiResponse<FullDiagnoseResponse> full(@Valid @RequestBody FullDiagnoseRequest request) {
        return ApiResponse.success(fullDiagnoseService.diagnose(request));
    }
}
