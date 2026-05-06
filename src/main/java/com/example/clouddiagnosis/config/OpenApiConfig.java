package com.example.clouddiagnosis.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cloudDiagnosisOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Web 应用故障诊断与运维支持平台 API")
                .description("面向 URL/API 连通性、HTTP 状态码、Header、DNS、网关和上游服务异常的轻量级诊断接口")
                .version("v1.0.0"));
    }
}
