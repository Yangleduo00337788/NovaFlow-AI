package ai.novaflow.dashboard.service;

import ai.novaflow.dashboard.domain.DashboardOverviewVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    public DashboardOverviewVO getOverview() {
        return DashboardOverviewVO.builder()
                .stats(List.of(
                        card("apps", "应用总数", "32", "+12%", true),
                        card("agents", "Agent 总数", "128", "+18%", true),
                        card("invocations", "调用次数", "1.2M", "+23%", true),
                        card("tokens", "Token 消耗", "348.6M", "+15%", true),
                        card("cost", "成本（元）", "¥12,586.32", "-8%", false)
                ))
                .recentItems(List.of(
                        recent("智能客服 Agent", "Agent", "2 小时前", "/agent"),
                        recent("合同审核工作流", "工作流", "5 小时前", "/workflow"),
                        recent("产品知识库", "知识库", "1 天前", "/knowledge"),
                        recent("数据分析 Agent", "Agent", "2 天前", "/agent"),
                        recent("文档助手工作流", "工作流", "3 天前", "/workflow")
                ))
                .recentLogs(List.of(
                        log("智能客服 Agent", true, "2 分钟前", "2.3s", 1256),
                        log("合同审查工作流", true, "5 分钟前", "4.8s", 3432),
                        log("财务报表解析", false, "15 分钟前", "-", 0),
                        log("企业知识问答", true, "30 分钟前", "1.6s", 987),
                        log("市场分析 Agent", true, "1 小时前", "3.2s", 2145)
                ))
                .modelUsage(List.of(
                        model("DeepSeek R1", 42, "146.1M"),
                        model("Qwen 2.5", 28, "97.6M"),
                        model("GPT-4o", 18, "62.7M"),
                        model("Claude 3.5", 12, "42.2M")
                ))
                .topApps(List.of(
                        top("智能客服 Agent", "128.6K"),
                        top("合同审查助手", "96.3K"),
                        top("财务分析 Agent", "72.1K"),
                        top("企业知识问答", "58.4K"),
                        top("数据洞察工作流", "41.2K")
                ))
                .systemHealth(List.of(
                        health("API 服务", true),
                        health("向量数据库", true),
                        health("消息队列", true),
                        health("存储服务", true)
                ))
                .trend(List.of(
                        trend("00:00", 3200), trend("04:00", 1800), trend("08:00", 8600),
                        trend("12:00", 15200), trend("16:00", 22532), trend("20:00", 12400)
                ))
                .quickActions(List.of(
                        action("api-key", "API Key 管理", "/settings/api-key"),
                        action("prompt", "Prompt 模板", "/prompt"),
                        action("dataset", "数据集管理", "/knowledge"),
                        action("mcp", "MCP 服务", "/tool/mcp"),
                        action("settings", "系统设置", "/settings"),
                        action("users", "用户管理", "/org/members")
                ))
                .planInfo(DashboardOverviewVO.PlanInfoVO.builder()
                        .planType("企业版")
                        .expireAt("2028-12-31")
                        .usedPercent(68)
                        .build())
                .build();
    }

    private DashboardOverviewVO.StatCardVO card(String key, String label, String value, String change, boolean up) {
        return DashboardOverviewVO.StatCardVO.builder().key(key).label(label).value(value).change(change).up(up).build();
    }

    private DashboardOverviewVO.RecentItemVO recent(String name, String type, String updatedAt, String path) {
        return DashboardOverviewVO.RecentItemVO.builder().name(name).type(type).updatedAt(updatedAt).path(path).build();
    }

    private DashboardOverviewVO.RecentLogVO log(String name, boolean success, String time, String duration, int tokens) {
        return DashboardOverviewVO.RecentLogVO.builder()
                .name(name).success(success).status(success ? "成功" : "失败")
                .time(time).duration(duration).tokens(tokens).build();
    }

    private DashboardOverviewVO.ModelUsageVO model(String model, int percent, String tokens) {
        return DashboardOverviewVO.ModelUsageVO.builder().model(model).percent(percent).tokens(tokens).build();
    }

    private DashboardOverviewVO.TopAppVO top(String name, String value) {
        return DashboardOverviewVO.TopAppVO.builder().name(name).value(value).build();
    }

    private DashboardOverviewVO.SystemHealthVO health(String name, boolean healthy) {
        return DashboardOverviewVO.SystemHealthVO.builder()
                .name(name).healthy(healthy).status(healthy ? "正常" : "异常").build();
    }

    private DashboardOverviewVO.TrendPointVO trend(String time, long value) {
        return DashboardOverviewVO.TrendPointVO.builder().time(time).value(value).build();
    }

    private DashboardOverviewVO.QuickActionVO action(String key, String label, String path) {
        return DashboardOverviewVO.QuickActionVO.builder().key(key).label(label).path(path).build();
    }
}
