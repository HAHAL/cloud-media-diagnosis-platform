package com.example.clouddiagnosis.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DnsDiagnoseRequest {
    @NotBlank(message = "域名不能为空")
    private String domain;

    private Boolean useCache = true;
}
