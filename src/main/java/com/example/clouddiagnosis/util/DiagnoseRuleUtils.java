package com.example.clouddiagnosis.util;

import com.example.clouddiagnosis.model.common.RiskLevel;

import java.util.List;

public final class DiagnoseRuleUtils {
    private DiagnoseRuleUtils() {
    }

    public static RiskLevel riskByStatus(Integer statusCode) {
        if (statusCode == null) {
            return RiskLevel.HIGH;
        }
        if (statusCode >= 500 || statusCode == 403 || statusCode == 404) {
            return RiskLevel.HIGH;
        }
        if (statusCode >= 300 || statusCode >= 400) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    public static void appendStatusDiagnosis(Integer statusCode, List<String> diagnosis, List<String> suggestions) {
        // 状态码是云产品排障第一分流点：先判断客户端、CDN 节点、回源链路还是源站服务方向。
        if (statusCode == null) {
            diagnosis.add("HTTP 请求失败，未获取到有效状态码");
            suggestions.add("检查 URL 是否可达、DNS 是否可解析、客户端到目标站点的网络连通性");
        } else if (statusCode >= 200 && statusCode < 300) {
            diagnosis.add("HTTP 状态码正常");
            suggestions.add("继续检查缓存命中率、源站响应时间和客户访问地域链路质量");
        } else if (statusCode >= 300 && statusCode < 400) {
            diagnosis.add("检测到 3xx 重定向响应");
            suggestions.add("检查重定向链路是否过长、Location 是否指向正确域名、HTTPS 跳转是否配置一致");
        } else if (statusCode == 403) {
            diagnosis.add("访问被拒绝，返回 403");
            suggestions.add("检查权限、防盗链、Referer 白名单、签名 URL 是否过期以及 WAF/ACL 规则");
        } else if (statusCode == 404) {
            diagnosis.add("资源不存在或 CDN 缓存了 404");
            suggestions.add("检查资源路径、源站文件是否存在、CDN 是否缓存了异常状态码");
        } else if (statusCode == 502) {
            diagnosis.add("网关或回源链路异常，返回 502");
            suggestions.add("检查源站端口、反向代理、TLS 握手、回源 Host 和源站健康状态");
        } else if (statusCode == 504) {
            diagnosis.add("回源或网关等待超时，返回 504");
            suggestions.add("检查源站响应耗时、回源链路质量、CDN 回源超时配置和源站负载");
        } else if (statusCode >= 500) {
            diagnosis.add("服务端或 CDN 回源异常，返回 5xx");
            suggestions.add("检查源站应用日志、负载均衡健康检查、CDN 节点回源错误日志");
        } else {
            diagnosis.add("HTTP 状态码异常：" + statusCode);
            suggestions.add("结合访问日志和 CDN 日志确认异常发生位置");
        }
    }

    public static RiskLevel max(RiskLevel... levels) {
        RiskLevel result = RiskLevel.LOW;
        for (RiskLevel level : levels) {
            if (level == RiskLevel.HIGH) {
                return RiskLevel.HIGH;
            }
            if (level == RiskLevel.MEDIUM) {
                result = RiskLevel.MEDIUM;
            }
        }
        return result;
    }
}
