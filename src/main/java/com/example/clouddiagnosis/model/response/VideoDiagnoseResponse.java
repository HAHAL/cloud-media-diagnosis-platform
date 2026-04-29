package com.example.clouddiagnosis.model.response;

import com.example.clouddiagnosis.model.common.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class VideoDiagnoseResponse {
    private String url;
    private String videoType;
    private Boolean videoContentType;
    private Boolean rangeSupported;
    private Boolean contentLengthExists;
    private Long contentLength;
    private Boolean maybeLargeFile;
    private RiskLevel riskLevel;
    private Map<String, String> headers;
    private List<String> diagnosis;
    private List<String> suggestions;
}
