package com.example.clouddiagnosis.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OriginCompareRequest {
    @NotBlank(message = "CDN URL 不能为空")
    private String cdnUrl;

    @NotBlank(message = "源站 URL 不能为空")
    private String originUrl;

    private String hostHeader;
}
