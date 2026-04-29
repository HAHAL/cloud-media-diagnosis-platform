package com.example.clouddiagnosis.model.response;

import com.example.clouddiagnosis.model.common.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FullDiagnoseResponse {
    private String url;
    private RiskLevel overallRiskLevel;
    private String summary;
    private List<String> rootCauseHints;
    private List<String> nextActions;
    private String customerReplyTemplate;
    private DnsDiagnoseResponse dns;
    private HttpDiagnoseResponse http;
    private CdnHeaderAnalyzeResponse cdnHeader;
    private VideoDiagnoseResponse video;
}
