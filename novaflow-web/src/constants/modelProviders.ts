import type { ModelProviderItem } from '@/api/model'

export const MODEL_PROVIDER_PRESETS: ModelProviderItem[] = [
  {
    providerCode: 'openai',
    providerName: 'OpenAI',
    description: 'GPT 系列模型，适合通用对话与推理',
    defaultBaseUrl: 'https://api.openai.com/v1',
    baseUrl: 'https://api.openai.com/v1',
    configured: false,
    enabled: false,
    modelCount: 0,
  },
  {
    providerCode: 'deepseek',
    providerName: 'DeepSeek',
    description: '国产高性价比大模型，适合对话与推理场景',
    defaultBaseUrl: 'https://api.deepseek.com/v1',
    baseUrl: 'https://api.deepseek.com/v1',
    configured: false,
    enabled: false,
    modelCount: 0,
  },
]

export function mergeModelProviders(apiList: ModelProviderItem[] = []): ModelProviderItem[] {
  const apiMap = new Map(apiList.map((item) => [item.providerCode, item]))
  return MODEL_PROVIDER_PRESETS.map((preset) => {
    const fromApi = apiMap.get(preset.providerCode)
    if (!fromApi) return { ...preset }
    return {
      ...preset,
      ...fromApi,
      description: fromApi.description || preset.description,
      defaultBaseUrl: fromApi.defaultBaseUrl || preset.defaultBaseUrl,
      baseUrl: fromApi.baseUrl || preset.baseUrl || preset.defaultBaseUrl,
    }
  })
}
