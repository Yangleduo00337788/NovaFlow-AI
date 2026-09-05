import type { PlatformUserMembership } from '@/api/platform'

export function formatPlatformNumber(value?: number) {
  return (value ?? 0).toLocaleString()
}

export function formatPlatformCost(value?: number) {
  return (value ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 4 })
}

export function formatMemberships(memberships: PlatformUserMembership[]) {
  return memberships
    .map((m) => `${m.tenantName || m.tenantId}(${m.roleName || m.roleCode || '-'})`)
    .join('、')
}

export const PLAN_OPTIONS = [
  { value: 'personal', label: '个人版' },
  { value: 'free', label: '免费版' },
  { value: 'starter', label: '入门版' },
  { value: 'pro', label: '专业版' },
  { value: 'enterprise', label: '企业版' },
]

export function formatPlatformDate(value?: string | null) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('zh-CN')
}

export function formatPlatformDateTime(value?: string | null) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN')
}

export function quotaPercent(used?: number, limit?: number) {
  if (!limit || limit <= 0) return null
  return Math.min(100, Math.round(((used ?? 0) * 100) / limit))
}
