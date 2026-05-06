package com.example.clouddiagnosis.service;

import com.example.clouddiagnosis.model.request.StatusCodeDiagnoseRequest;
import com.example.clouddiagnosis.model.response.StatusCodeDiagnoseResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatusCodeDiagnoseService {

    public StatusCodeDiagnoseResponse diagnose(StatusCodeDiagnoseRequest request) {
        int code = request.getStatusCode();
        return switch (code) {
            case 400 -> build(code, "BAD_REQUEST", "medium",
                    "请求返回 400，通常表示请求参数、JSON 格式或必填字段不符合接口约束。",
                    List.of("请求参数错误", "JSON 格式错误", "必填字段缺失", "字段类型与接口定义不一致"),
                    List.of("对照接口文档检查请求体和 Query 参数", "确认 Content-Type 是否正确", "查看应用参数校验日志", "保留一份失败请求样例用于复现"),
                    List.of("完整请求参数", "接口路径", "请求方法", "错误响应体"));
            case 401 -> build(code, "UNAUTHORIZED", "medium",
                    "请求返回 401，通常表示认证失败或认证信息缺失。",
                    List.of("未登录", "Token 过期", "Authorization Header 缺失", "认证服务校验失败"),
                    List.of("检查 Authorization Header 是否传递", "确认 Token 是否过期或被撤销", "查看认证网关和应用认证日志", "确认客户端时间与服务端时间是否偏差过大"),
                    List.of("用户标识", "Token 生成时间", "请求时间", "认证错误响应"));
            case 403 -> build(code, "FORBIDDEN", "high",
                    "请求返回 403，通常表示身份已识别但没有访问权限。",
                    List.of("权限不足", "角色策略不匹配", "IP 白名单限制", "访问控制或鉴权策略异常"),
                    List.of("检查用户角色和资源权限", "确认 IP 白名单或访问控制策略", "查看网关、WAF 或应用鉴权日志", "确认是否命中防护规则"),
                    List.of("用户 ID", "角色信息", "客户端 IP", "接口路径", "requestId"));
            case 404 -> build(code, "NOT_FOUND", "medium",
                    "请求返回 404，通常表示接口路径、资源路径或路由转发配置不正确。",
                    List.of("接口路径错误", "资源不存在", "路由配置错误", "Nginx 转发路径错误"),
                    List.of("确认请求路径和 HTTP 方法是否正确", "检查网关或 Nginx location 配置", "确认应用是否部署了对应接口", "查看 access log 中实际转发路径"),
                    List.of("请求路径", "HTTP 方法", "部署版本", "网关转发配置"));
            case 408 -> build(code, "REQUEST_TIMEOUT", "medium",
                    "请求返回 408，通常表示客户端请求超时或服务响应过慢。",
                    List.of("客户端网络不稳定", "请求体上传过慢", "服务处理时间过长", "链路中代理超时"),
                    List.of("检查客户端网络和超时配置", "查看网关 request_time", "确认请求体大小", "对慢请求做链路追踪"),
                    List.of("请求时间", "客户端网络环境", "请求体大小", "requestId"));
            case 429 -> build(code, "TOO_MANY_REQUESTS", "medium",
                    "请求返回 429，通常表示触发限流、频控或 API 配额限制。",
                    List.of("限流", "频控", "API 配额不足", "并发过高"),
                    List.of("查看限流规则和当前 QPS", "确认客户端是否有重试风暴", "检查调用方配额", "必要时临时调整限流阈值或削峰"),
                    List.of("调用方标识", "请求频率", "限流规则", "影响范围"));
            case 500 -> build(code, "INTERNAL_SERVER_ERROR", "high",
                    "请求返回 500，通常表示应用内部异常或依赖组件异常。",
                    List.of("应用异常", "代码异常", "数据库异常", "配置异常"),
                    List.of("根据 requestId 检索应用日志", "检查异常堆栈和错误码", "查看数据库连接和慢查询", "确认近期发布或配置变更"),
                    List.of("requestId", "请求时间", "异常堆栈", "发布版本", "完整错误响应"));
            case 502 -> build(code, "BAD_GATEWAY", "high",
                    "请求返回 502，通常表示网关无法获得有效的上游服务响应。",
                    List.of("上游服务未启动", "端口不可达", "upstream 配置错误", "上游连接被拒绝或提前关闭"),
                    List.of("查看 Nginx 或网关 error log", "确认上游服务端口监听状态", "检查 upstream 地址和协议", "确认健康检查是否通过"),
                    List.of("网关错误日志", "upstream 地址", "服务实例状态", "请求时间"));
            case 503 -> build(code, "SERVICE_UNAVAILABLE", "high",
                    "请求返回 503，通常表示服务当前不可用或实例未通过健康检查。",
                    List.of("服务不可用", "实例下线", "发布中", "健康检查失败"),
                    List.of("检查服务实例和健康检查状态", "确认是否处于发布或扩缩容窗口", "查看注册中心或负载均衡状态", "检查资源水位和限流保护"),
                    List.of("实例列表", "健康检查结果", "发布时间", "影响范围"));
            case 504 -> build(code, "GATEWAY_TIMEOUT", "high",
                    "接口返回 504，通常表示网关等待上游服务响应超时。",
                    List.of("上游服务处理时间过长", "数据库慢查询", "第三方接口响应超时", "线程池或连接池耗尽"),
                    List.of("查看网关或 Nginx access log 中的 upstream_response_time", "根据 requestId 检索应用日志", "检查数据库慢查询和连接池状态", "确认近期是否有发布或配置变更"),
                    List.of("请求时间", "requestId", "接口路径", "影响范围", "完整错误响应"));
            default -> build(code, "HTTP_STATUS_" + code, code >= 500 ? "high" : "medium",
                    "该状态码需要结合接口路径、网关日志、应用日志和请求上下文进一步判断。",
                    List.of("业务逻辑返回异常状态", "网关或代理规则影响", "依赖服务响应异常"),
                    List.of("确认状态码由网关还是应用返回", "检查 access log 和应用日志", "补充 requestId 后进行链路定位"),
                    List.of("请求时间", "requestId", "接口路径", "完整响应"));
        };
    }

    private StatusCodeDiagnoseResponse build(int code, String category, String severity, String summary,
                                             List<String> possibleCauses, List<String> steps, List<String> needMoreInfo) {
        return StatusCodeDiagnoseResponse.builder()
                .statusCode(code)
                .category(category)
                .severity(severity)
                .summary(summary)
                .possibleCauses(possibleCauses)
                .troubleshootingSteps(steps)
                .needMoreInfo(needMoreInfo)
                .build();
    }
}
