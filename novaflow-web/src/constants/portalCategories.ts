export const PORTAL_CATEGORY_OPTIONS = [
  { value: 'general', label: '通用' },
  { value: 'office', label: '办公效率' },
  { value: 'customer_service', label: '客户服务' },
  { value: 'knowledge', label: '知识问答' },
  { value: 'writing', label: '内容创作' },
] as const

export function portalCategoryLabel(code?: string) {
  if (!code) return '通用'
  return PORTAL_CATEGORY_OPTIONS.find((item) => item.value === code)?.label || code
}
