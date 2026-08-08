package ai.novaflow.model.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum ModelProviderPreset {

    OPENAI(
            "openai",
            "OpenAI",
            "https://api.openai.com/v1",
            "GPT 系列，适合通用对话、推理与 Embedding",
            BillingCurrency.USD,
            ProviderRegion.INTERNATIONAL,
            ProviderApiStyle.OPENAI_COMPATIBLE,
            true
    ),
    DEEPSEEK(
            "deepseek",
            "DeepSeek",
            "https://api.deepseek.com/v1",
            "国产高性价比大模型，适合对话与推理",
            BillingCurrency.CNY,
            ProviderRegion.DOMESTIC,
            ProviderApiStyle.OPENAI_COMPATIBLE,
            true
    ),
    QWEN(
            "qwen",
            "通义千问",
            "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "阿里云通义系列，支持对话与 Embedding",
            BillingCurrency.CNY,
            ProviderRegion.DOMESTIC,
            ProviderApiStyle.OPENAI_COMPATIBLE,
            true
    ),
    MOONSHOT(
            "moonshot",
            "Moonshot / Kimi",
            "https://api.moonshot.cn/v1",
            "月之暗面 Kimi 长上下文模型",
            BillingCurrency.CNY,
            ProviderRegion.DOMESTIC,
            ProviderApiStyle.OPENAI_COMPATIBLE,
            true
    ),
    ZHIPU(
            "zhipu",
            "智谱 AI",
            "https://open.bigmodel.cn/api/paas/v4",
            "GLM 系列，支持对话与 Embedding",
            BillingCurrency.CNY,
            ProviderRegion.DOMESTIC,
            ProviderApiStyle.OPENAI_COMPATIBLE,
            true
    ),
    BAICHUAN(
            "baichuan",
            "百川智能",
            "https://api.baichuan-ai.com/v1",
            "百川大模型，中文场景表现优秀",
            BillingCurrency.CNY,
            ProviderRegion.DOMESTIC,
            ProviderApiStyle.OPENAI_COMPATIBLE,
            true
    ),
    MINIMAX(
            "minimax",
            "MiniMax",
            "https://api.minimax.chat/v1",
            "海螺大模型，支持长文本与语音场景",
            BillingCurrency.CNY,
            ProviderRegion.DOMESTIC,
            ProviderApiStyle.OPENAI_COMPATIBLE,
            true
    ),
    DOUBAO(
            "doubao",
            "豆包 / 火山引擎",
            "https://ark.cn-beijing.volces.com/api/v3",
            "字节跳动豆包大模型（火山方舟）",
            BillingCurrency.CNY,
            ProviderRegion.DOMESTIC,
            ProviderApiStyle.CATALOG_ONLY,
            true
    ),
    BAIDU(
            "baidu",
            "文心一言",
            "https://qianfan.baidubce.com/v2",
            "百度千帆大模型平台",
            BillingCurrency.CNY,
            ProviderRegion.DOMESTIC,
            ProviderApiStyle.CATALOG_ONLY,
            true
    ),
    SILICONFLOW(
            "siliconflow",
            "硅基流动",
            "https://api.siliconflow.cn/v1",
            "聚合多家开源与商业模型，性价比高",
            BillingCurrency.CNY,
            ProviderRegion.AGGREGATOR,
            ProviderApiStyle.OPENAI_COMPATIBLE,
            true
    ),
    CLAUDE(
            "claude",
            "Anthropic Claude",
            "https://api.anthropic.com/v1",
            "Claude 系列，擅长长文本与代码（需配置兼容网关或手动维护模型）",
            BillingCurrency.USD,
            ProviderRegion.INTERNATIONAL,
            ProviderApiStyle.CATALOG_ONLY,
            true
    ),
    GEMINI(
            "gemini",
            "Google Gemini",
            "https://generativelanguage.googleapis.com/v1beta/openai",
            "Gemini 系列，Google OpenAI 兼容端点",
            BillingCurrency.USD,
            ProviderRegion.INTERNATIONAL,
            ProviderApiStyle.OPENAI_COMPATIBLE,
            true
    ),
    OLLAMA(
            "ollama",
            "Ollama",
            "http://localhost:11434/v1",
            "本地部署开源模型，无需 API Key",
            BillingCurrency.CNY,
            ProviderRegion.LOCAL,
            ProviderApiStyle.OPENAI_COMPATIBLE,
            false
    ),
    CUSTOM(
            "custom",
            "自定义",
            "",
            "任意 OpenAI 兼容 API（OneAPI、New API、私有网关等）",
            BillingCurrency.USD,
            ProviderRegion.AGGREGATOR,
            ProviderApiStyle.OPENAI_COMPATIBLE,
            true
    );

    private final String code;
    private final String name;
    private final String defaultBaseUrl;
    private final String description;
    private final BillingCurrency currency;
    private final ProviderRegion region;
    private final ProviderApiStyle apiStyle;
    private final boolean requiresApiKey;

    public static Optional<ModelProviderPreset> of(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst();
    }

    public static List<ModelProviderPreset> all() {
        return List.of(values());
    }
}
