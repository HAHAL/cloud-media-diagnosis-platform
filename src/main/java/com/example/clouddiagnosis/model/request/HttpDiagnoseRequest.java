package com.example.clouddiagnosis.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HttpDiagnoseRequest {
    @NotBlank(message = "URL 不能为空")
    private String url;

    @Min(value = 1000, message = "超时时间不能小于 1000ms")
    @Max(value = 15000, message = "超时时间不能大于 15000ms")
    private Integer timeoutMs = 5000;

    private Boolean followRedirect = true;
}
