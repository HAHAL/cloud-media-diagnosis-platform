let lastRaw = null;
let lastReportText = "";

const forms = {
  health: document.querySelector("#health-form"),
  status: document.querySelector("#status-form"),
  header: document.querySelector("#header-form"),
  full: document.querySelector("#full-form"),
  history: document.querySelector("#history-form")
};

document.querySelectorAll(".tab").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach((tab) => tab.classList.remove("active"));
    Object.values(forms).forEach((form) => form.classList.remove("active"));
    button.classList.add("active");
    forms[button.dataset.tab].classList.add("active");
    if (button.dataset.tab === "history") {
      loadHistory();
    }
  });
});

forms.health.addEventListener("submit", async (event) => {
  event.preventDefault();
  const data = formData(forms.health);
  data.timeoutMs = Number(data.timeoutMs);
  const result = await callApi("/api/diagnose/http", data);
  renderHttp(result);
});

forms.status.addEventListener("submit", async (event) => {
  event.preventDefault();
  const data = formData(forms.status);
  data.statusCode = Number(data.statusCode);
  const result = await callApi("/api/diagnose/status-code", data);
  renderStatus(result);
});

forms.header.addEventListener("submit", async (event) => {
  event.preventDefault();
  const result = await callApi("/api/diagnose/header", formData(forms.header));
  renderHeader(result);
});

forms.full.addEventListener("submit", async (event) => {
  event.preventDefault();
  const data = formData(forms.full);
  if (!data.originUrl) {
    delete data.originUrl;
  }
  const result = await callApi("/api/diagnose/full", data);
  renderFull(result);
});

document.querySelector("#load-history").addEventListener("click", loadHistory);
document.querySelector("#toggle-json").addEventListener("click", () => {
  document.querySelector("#raw-json").classList.toggle("hidden");
});
document.querySelector("#copy-report").addEventListener("click", async () => {
  if (!lastReportText) {
    showMessage("暂无可复制的诊断报告。", "message");
    return;
  }
  await navigator.clipboard.writeText(lastReportText);
  showMessage("诊断报告已复制。", "message");
});

function formData(form) {
  const data = {};
  new FormData(form).forEach((value, key) => {
    data[key] = value;
  });
  form.querySelectorAll("input[type='checkbox']").forEach((input) => {
    data[input.name] = input.checked;
  });
  return data;
}

async function callApi(url, body) {
  showMessage("诊断执行中...", "message");
  try {
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    const payload = await response.json();
    lastRaw = payload;
    document.querySelector("#raw-json").textContent = JSON.stringify(payload, null, 2);
    if (payload.code !== 0) {
      renderError(payload.message || "接口调用失败");
      return null;
    }
    showMessage("", "");
    return payload.data;
  } catch (error) {
    lastRaw = { error: error.message };
    document.querySelector("#raw-json").textContent = JSON.stringify(lastRaw, null, 2);
    renderError("接口调用失败：" + error.message);
    return null;
  }
}

function renderHttp(data) {
  if (!data) return;
  const headers = data.headers || {};
  const summary = (data.diagnosis || []).join("；") || "URL/API 健康检查已完成。";
  const suggestions = data.suggestions || [];
  renderReport({
    category: "URL/API 健康检查",
    severity: data.riskLevel,
    statusCode: data.statusCode,
    responseTimeMs: data.responseTimeMs,
    cached: data.cached,
    summary,
    possibleCauses: data.diagnosis || [],
    steps: suggestions,
    needMoreInfo: ["请求时间", "客户端 IP", "requestId/traceId", "完整错误响应"],
    metrics: {
      "Content-Type": headers["Content-Type"] || "-",
      "Server": headers.Server || "-",
      "跟随重定向": String(data.redirect),
      "Range 支持": String(data.rangeSupported)
    }
  });
}

function renderStatus(data) {
  if (!data) return;
  renderReport({
    category: data.category,
    severity: data.severity === "high" ? "HIGH" : "MEDIUM",
    statusCode: data.statusCode,
    responseTimeMs: "-",
    cached: "-",
    summary: data.summary,
    possibleCauses: data.possibleCauses,
    steps: data.troubleshootingSteps,
    needMoreInfo: data.needMoreInfo,
    metrics: {}
  });
}

function renderHeader(data) {
  if (!data) return;
  const headers = data.headers || {};
  renderReport({
    category: "HTTP Header 分析",
    severity: data.riskLevel,
    statusCode: data.statusCode,
    responseTimeMs: "-",
    cached: data.cached,
    summary: (data.diagnosis || []).join("；") || "Header 分析已完成。",
    possibleCauses: data.diagnosis || [],
    steps: data.suggestions || [],
    needMoreInfo: ["响应头完整截图", "调用方域名", "浏览器控制台错误", "网关日志"],
    metrics: {
      "Content-Type": headers["Content-Type"] || "-",
      "Cache-Control": headers["Cache-Control"] || "-",
      "Set-Cookie": headers["Set-Cookie"] ? "存在" : "无",
      "Location": headers.Location || "-",
      "Server": headers.Server || "-",
      "CORS": headers["Access-Control-Allow-Origin"] || "-",
      "安全 Header": headers["Strict-Transport-Security"] || headers["Content-Security-Policy"] || "-"
    }
  });
}

function renderFull(data) {
  if (!data) return;
  renderReport({
    category: "综合诊断报告",
    severity: data.overallRiskLevel,
    statusCode: data.http && data.http.statusCode,
    responseTimeMs: data.http && data.http.responseTimeMs,
    cached: data.cached,
    summary: data.summary,
    possibleCauses: data.rootCauseHints || [],
    steps: data.nextActions || [],
    needMoreInfo: ["请求时间", "requestId/traceId", "影响范围", "完整错误响应", "部署或配置变更记录"],
    metrics: {
      "记录 ID": data.recordId || "-",
      "DNS IP 数": data.dns && data.dns.ips ? data.dns.ips.length : "-",
      "Header 缓存状态": data.cdnHeader ? data.cdnHeader.cacheStatus : "-",
      "缓存 Key": data.cacheKey || "-"
    }
  });
}

function renderReport(model) {
  const metrics = {
    "问题分类": model.category,
    "严重级别": model.severity,
    "状态码": model.statusCode ?? "-",
    "响应时间": model.responseTimeMs === "-" ? "-" : `${model.responseTimeMs ?? "-"} ms`,
    "是否命中缓存": String(model.cached ?? "-"),
    ...model.metrics
  };
  const html = `
    <div class="metric-grid">${Object.entries(metrics).map(([key, value]) => metric(key, value, key === "严重级别" ? model.severity : "")).join("")}</div>
    ${section("问题摘要", [model.summary])}
    ${section("可能原因", model.possibleCauses)}
    ${section("排查步骤", model.steps)}
    ${section("需要补充的信息", model.needMoreInfo)}
    ${section("建议处理方向", ["先确认状态码和响应耗时归属，再结合网关日志、应用日志、数据库或第三方依赖耗时逐层排查。"])}
  `;
  document.querySelector("#report-card").className = "report-card";
  document.querySelector("#report-card").innerHTML = html;
  lastReportText = document.querySelector("#report-card").innerText;
}

function metric(label, value, risk) {
  const badgeClass = risk ? `badge risk-${risk}` : "";
  const display = risk ? `<span class="${badgeClass}">${value}</span>` : `<strong>${escapeHtml(String(value))}</strong>`;
  return `<div class="metric"><span>${label}</span>${display}</div>`;
}

function section(title, items = []) {
  const list = (items || []).filter(Boolean);
  if (!list.length) {
    return "";
  }
  return `<div class="section"><h3>${title}</h3><ul>${list.map((item) => `<li>${escapeHtml(String(item))}</li>`).join("")}</ul></div>`;
}

async function loadHistory() {
  const params = new URLSearchParams();
  const url = document.querySelector("#history-url").value;
  const risk = document.querySelector("#history-risk").value;
  if (url) params.set("url", url);
  if (risk) params.set("riskLevel", risk);
  try {
    const response = await fetch(`/api/diagnose/history?${params.toString()}`);
    const payload = await response.json();
    if (payload.code !== 0) {
      document.querySelector("#history-list").innerHTML = `<div class="message error">诊断历史功能依赖 MySQL，请确认数据库服务已启动。${escapeHtml(payload.message || "")}</div>`;
      return;
    }
    const records = payload.data.content || [];
    document.querySelector("#history-list").innerHTML = records.length ? records.map(historyItem).join("") : "<div class='message'>暂无诊断历史记录。</div>";
    document.querySelectorAll("[data-detail]").forEach((button) => button.addEventListener("click", () => loadDetail(button.dataset.detail)));
    document.querySelectorAll("[data-delete]").forEach((button) => button.addEventListener("click", () => deleteHistory(button.dataset.delete)));
  } catch (error) {
    document.querySelector("#history-list").innerHTML = `<div class="message error">诊断历史功能依赖 MySQL，请确认数据库服务已启动。${escapeHtml(error.message)}</div>`;
  }
}

function historyItem(item) {
  return `
    <div class="history-item">
      <h4>${escapeHtml(item.requestUrl)}</h4>
      <div>状态码：${item.statusCode ?? "-"} ｜ 风险：${item.riskLevel || "-"} ｜ 类型：${item.diagnosisType} ｜ 时间：${item.createdAt}</div>
      <div class="history-actions">
        <button data-detail="${item.id}" type="button">查看详情</button>
        <button data-delete="${item.id}" type="button">删除记录</button>
      </div>
    </div>
  `;
}

async function loadDetail(id) {
  const response = await fetch(`/api/diagnose/history/${id}`);
  const payload = await response.json();
  lastRaw = payload;
  document.querySelector("#raw-json").textContent = JSON.stringify(payload, null, 2);
  if (payload.code !== 0) {
    renderError(payload.message);
    return;
  }
  const record = payload.data;
  renderReport({
    category: "诊断历史详情",
    severity: record.riskLevel,
    statusCode: record.statusCode,
    responseTimeMs: "-",
    cached: "-",
    summary: record.summary,
    possibleCauses: [record.requestUrl],
    steps: (record.suggestions || "").split("\n"),
    needMoreInfo: ["关联工单号", "请求时间", "requestId/traceId"],
    metrics: { "记录 ID": record.id, "诊断类型": record.diagnosisType, "创建时间": record.createdAt }
  });
}

async function deleteHistory(id) {
  const response = await fetch(`/api/diagnose/history/${id}`, { method: "DELETE" });
  const payload = await response.json();
  if (payload.code !== 0) {
    renderError(payload.message);
    return;
  }
  loadHistory();
}

function renderError(message) {
  document.querySelector("#report-card").className = "report-card";
  document.querySelector("#report-card").innerHTML = `<div class="message error">${escapeHtml(message)}</div>`;
  lastReportText = message;
}

function showMessage(text, className) {
  document.querySelector("#message").innerHTML = text ? `<div class="${className}">${escapeHtml(text)}</div>` : "";
}

function escapeHtml(value) {
  return value.replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;"
  }[char]));
}
