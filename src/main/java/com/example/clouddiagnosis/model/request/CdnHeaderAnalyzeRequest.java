package com.example.clouddiagnosis.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CdnHeaderAnalyzeRequest {
    @NotBlank(message = "URL 不能为空")
    private String url;

    private Boolean useCache = true;
}
