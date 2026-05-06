package com.example.clouddiagnosis.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusCodeDiagnoseRequest {
    @NotNull(message = "状态码不能为空")
    @Min(value = 100, message = "状态码不能小于 100")
    @Max(value = 599, message = "状态码不能大于 599")
    private Integer statusCode;

    private String errorMessage;

    private String path;
}
