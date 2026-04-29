package com.example.clouddiagnosis.service;

import cn.hutool.core.util.StrUtil;
import com.example.clouddiagnosis.exception.BizException;
import com.example.clouddiagnosis.model.common.RiskLevel;
import com.example.clouddiagnosis.model.request.DnsDiagnoseRequest;
import com.example.clouddiagnosis.model.response.DnsDiagnoseResponse;
import com.example.clouddiagnosis.util.CommandUtils;
import com.example.clouddiagnosis.util.UrlUtils;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DnsDiagnoseService {
    private static final List<String> CDN_CNAME_KEYWORDS = List.of(
            "cdn", "alicdn", "cloudfront", "edgekey", "akamai", "volc", "bytedance", "tencent", "wsdvs"
    );

    public DnsDiagnoseResponse diagnose(DnsDiagnoseRequest request) {
        String domain = request.getDomain();
        if (!UrlUtils.isValidDomain(domain)) {
            throw new BizException("域名格式不合法");
        }
        long start = System.nanoTime();
        Set<String> ips = new LinkedHashSet<>();
        List<String> diagnosis = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        try {
            Arrays.stream(InetAddress.getAllByName(domain))
                    .map(InetAddress::getHostAddress)
                    .forEach(ips::add);
        } catch (Exception ex) {
            diagnosis.add("A 记录解析失败：" + ex.getMessage());
        }

        List<String> cnames = resolveCnames(domain);
        boolean cdnLike = cnames.stream().map(item -> item.toLowerCase(Locale.ROOT))
                .anyMatch(item -> CDN_CNAME_KEYWORDS.stream().anyMatch(item::contains));
        long cost = (System.nanoTime() - start) / 1_000_000;
        RiskLevel riskLevel = ips.isEmpty() ? RiskLevel.HIGH : RiskLevel.LOW;

        if (ips.isEmpty()) {
            diagnosis.add("未获取到 A 记录解析结果，疑似 DNS 配置异常或解析链路不可用");
            suggestions.add("检查权威 DNS 配置、CNAME 是否生效、本地递归 DNS 是否缓存异常");
        } else {
            diagnosis.add("域名解析成功，返回 IP 数量：" + ips.size());
        }
        if (ips.size() > 1) {
            diagnosis.add("解析到多个 IP，可能存在负载均衡、GSLB 调度或 CDN 节点调度");
            suggestions.add("建议按客户地域和运营商分别解析，确认是否调度到预期节点");
        }
        if (cdnLike) {
            diagnosis.add("CNAME 命中 CDN 关键词，疑似已接入 CDN");
            suggestions.add("继续检查加速域名配置、缓存规则和回源 Host 是否正确");
        } else if (cnames.isEmpty()) {
            suggestions.add("未获取到 CNAME，若业务预期接入 CDN，请确认域名是否已正确 CNAME 到加速域名");
        }
        if (cost > 1000) {
            riskLevel = RiskLevel.MEDIUM;
            diagnosis.add("DNS 解析耗时偏高，可能影响首包和首帧时间");
        }

        return DnsDiagnoseResponse.builder()
                .domain(domain)
                .ips(new ArrayList<>(ips))
                .cnames(cnames)
                .resolveTimeMs(cost)
                .cdnLike(cdnLike)
                .riskLevel(riskLevel)
                .diagnosis(diagnosis)
                .suggestions(suggestions)
                .build();
    }

    private List<String> resolveCnames(String domain) {
        return CommandUtils.run(Duration.ofSeconds(2), "dig", "+short", "CNAME", domain)
                .or(() -> CommandUtils.run(Duration.ofSeconds(2), "nslookup", "-type=CNAME", domain))
                .map(lines -> lines.stream()
                        .map(String::trim)
                        .filter(StrUtil::isNotBlank)
                        .filter(line -> !line.startsWith(";") && !line.toLowerCase(Locale.ROOT).contains("server"))
                        .map(line -> line.replace("canonical name =", "").trim())
                        .map(line -> line.endsWith(".") ? line.substring(0, line.length() - 1) : line)
                        .distinct()
                        .toList())
                .orElseGet(List::of);
    }
}
