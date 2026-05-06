package com.example.clouddiagnosis.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FullDiagnoseRequest {
    @NotBlank(message = "URL 不能为空")
    private String url;

    private String originUrl;

    private String scenario = "web_api";

    private Boolean saveRecord = true;

    private Boolean useCache = true;
}
