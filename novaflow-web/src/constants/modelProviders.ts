import type { ModelProviderItem } from '@/api/model'

function preset(
  providerCode: string,
  providerName: string,
  description: string,
  defaultBaseUrl: string,
  region: ModelProviderItem['region'] = 'international',
  apiStyle: ModelProviderItem['apiStyle'] = 'openai_compatible',
  requiresApiKey = true,
): ModelProviderItem {
  return {
    providerCode,
    providerName,
    description,
    defaultBaseUrl,
    baseUrl: defaultBaseUrl,
    region,
    apiStyle,
    requiresApiKey,
    configured: false,
    enabled: false,
    modelCount: 0,
  }
}

export const MODEL_PROVIDER_PRESETS: ModelProviderItem[] = [
  preset('openai', 'OpenAI', 'GPT 系列，适合通用对话、推理与 Embedding', 'https://api.openai.com/v1', 'international'),
  preset('deepseek', 'DeepSeek', '国产高性价比大模型，适合对话与推理', 'https://api.deepseek.com/v1', 'domestic'),
  preset('qwen', '通义千问', '阿里云通义系列，支持对话与 Embedding', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'domestic'),
  preset('moonshot', 'Moonshot / Kimi', '月之暗面 Kimi 长上下文模型', 'https://api.moonshot.cn/v1', 'domestic'),
  preset('zhipu', '智谱 AI', 'GLM 系列，支持对话与 Embedding', 'https://open.bigmodel.cn/api/paas/v4', 'domestic'),
  preset('baichuan', '百川智能', '百川大模型，中文场景表现优秀', 'https://api.baichuan-ai.com/v1', 'domestic'),
  preset('minimax', 'MiniMax', '海螺大模型，支持长文本场景', 'https://api.minimax.chat/v1', 'domestic'),
  preset('doubao', '豆包 / 火山引擎', '字节跳动豆包大模型（火山方舟）', 'https://ark.cn-beijing.volces.com/api/v3', 'domestic', 'catalog_only'),
  preset('baidu', '文心一言', '百度千帆大模型平台', 'https://qianfan.baidubce.com/v2', 'domestic', 'catalog_only'),
  preset('siliconflow', '硅基流动', '聚合多家开源与商业模型', 'https://api.siliconflow.cn/v1', 'aggregator'),
  preset('claude', 'Anthropic Claude', 'Claude 系列，擅长长文本与代码', 'https://api.anthropic.com/v1', 'international', 'catalog_only'),
  preset('gemini', 'Google Gemini', 'Gemini 系列 OpenAI 兼容端点', 'https://generativelanguage.googleapis.com/v1beta/openai', 'international'),
  preset('ollama', 'Ollama', '本地部署开源模型，无需 API Key', 'http://localhost:11434/v1', 'local', 'openai_compatible', false),
  preset('custom', '自定义', '任意 OpenAI 兼容 API（OneAPI、New API 等）', '', 'aggregator'),
]

export function mergeModelProviders(apiList: ModelProviderItem[] = []): ModelProviderItem[] {
  const apiMap = new Map(apiList.map((item) => [item.providerCode, item]))
  return MODEL_PROVIDER_PRESETS.map((presetItem) => {
    const fromApi = apiMap.get(presetItem.providerCode)
    if (!fromApi) return { ...presetItem }
    return {
      ...presetItem,
      ...fromApi,
      description: fromApi.description || presetItem.description,
      defaultBaseUrl: fromApi.defaultBaseUrl || presetItem.defaultBaseUrl,
      baseUrl: fromApi.baseUrl || presetItem.baseUrl || presetItem.defaultBaseUrl,
      region: fromApi.region || presetItem.region,
      apiStyle: fromApi.apiStyle || presetItem.apiStyle,
      requiresApiKey: fromApi.requiresApiKey ?? presetItem.requiresApiKey,
    }
  })
}

export const PROVIDER_REGION_LABELS: Record<string, string> = {
  all: '全部',
  domestic: '国产',
  international: '国际',
  local: '本地',
  aggregator: '聚合/自定义',
}
