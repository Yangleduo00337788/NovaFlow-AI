import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface PlatformTenant {
  id: number
  tenantCode: string
  tenantName: string
  contactName?: string
  contactEmail?: string
  contactPhone?: string
  planType: string
  planTypeLabel: string
  status?: number
  expireAt?: string
  maxMembers?: number
  maxAgents?: number
  maxKnowledge?: number
  maxStorageMb?: number
  monthlyTokenQuota?: number
  memberCount?: number
  usedTokensThisMonth?: number
  usedStorageBytes?: number
  storageUsedPercent?: number
  createdAt?: string
  updatedAt?: string
}

export interface PlatformTenantDetail {
  tenant: PlatformTenant
  agentCount: number
  knowledgeCount: number
  applicationCount: number
  workflowCount: number
  memberUsedPercent?: number
  tokenUsedPercent?: number
  storageUsedPercent?: number
  callsThisMonth: number
  costCnyThisMonth: number
  expired: boolean
  daysUntilExpiry?: number
  dailyTokenTrend: PlatformTrendPoint[]
  topModelsThisMonth: PlatformModelUsage[]
}

export interface PlatformTenantPage {
  list: PlatformTenant[]
  total: number
  page: number
  pageSize: number
}

export interface PlatformGlobalStats {
  tenantCount: number
  activeTenantCount: number
  totalMembers: number
  totalUsers: number
  totalAgents: number
  totalKnowledgeBases: number
  totalWorkflows: number
  tokensUsedThisMonth: number
}

export interface PlatformTenantHealth {
  tenantId: number
  tenantName: string
  healthStatus: 'HEALTHY' | 'WARNING' | 'CRITICAL' | string
  reasons: string[]
  tokenUsedPercent?: number
  memberUsedPercent?: number
  daysUntilExpiry?: number
  status?: number
}

export interface PlatformDashboardOverview {
  stats: PlatformGlobalStats
  tenantGrowthTrend: PlatformTrendPoint[]
  tokenUsageTrend: PlatformTrendPoint[]
  tenantHealth: PlatformTenantHealth[]
}

export interface PlatformUserMembership {
  tenantId: number
  tenantName?: string
  roleCode?: string
  roleName?: string
  status?: number
}

export interface PlatformUser {
  id: number
  username: string
  email: string
  nickname?: string
  accountType?: string
  status?: number
  lastLoginAt?: string
  lastLoginIp?: string
  createdAt?: string
  memberships?: PlatformUserMembership[]
}

export interface PlatformUserPage {
  list: PlatformUser[]
  total: number
  page: number
  pageSize: number
}

export interface PlatformLoginLog {
  id: number
  tenantId?: number
  userId?: number
  action: string
  resourceType?: string
  resourceId?: number
  detail?: string
  clientIp?: string
  createdAt?: string
}

export interface PlatformLoginLogPage {
  list: PlatformLoginLog[]
  total: number
  page: number
  pageSize: number
}

export interface PlatformTrendPoint {
  label: string
  tokens: number
}

export interface PlatformTenantUsage {
  tenantId: number
  tenantName?: string
  calls: number
  tokens: number
}

export interface PlatformModelUsage {
  modelName?: string
  displayName?: string
  calls: number
  tokens: number
}

export interface PlatformBillingOverview {
  month: string
  totalTokens: number
  prevMonthTokens: number
  totalCalls: number
  costCny: number
  costUsd: number
  dailyTrend: PlatformTrendPoint[]
  topTenants: PlatformTenantUsage[]
  topModels: PlatformModelUsage[]
}

export interface PlatformProviderStat {
  providerCode: string
  count: number
}

export interface PlatformModelOverview {
  totalProviders: number
  enabledProviders: number
  totalModelConfigs: number
  enabledModelConfigs: number
  providersByCode: PlatformProviderStat[]
}

export interface PlatformModelProvider {
  id: number
  tenantId: number
  tenantName?: string
  providerCode: string
  providerName?: string
  baseUrl?: string
  apiKeyMasked?: string
  enabled?: boolean
  modelCount?: number
  enabledModelCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface PlatformModelProviderPage {
  list: PlatformModelProvider[]
  total: number
  page: number
  pageSize: number
}

export interface IpBlacklistItem {
  id: number
  ipAddress: string
  reason?: string
  status?: number
  expireAt?: string
  createdBy?: number
  createdAt?: string
  updatedAt?: string
}

export interface IpBlacklistPage {
  list: IpBlacklistItem[]
  total: number
  page: number
  pageSize: number
}

export interface PlatformSettings {
  registrationEnabled: boolean
  hourlyCallsThreshold: number
  trafficSpikeMultiplier: number
  allowedProviderCodes: string[]
  providerWhitelistEnabled: boolean
  maintenanceEnabled?: boolean
  maintenanceMessage?: string
  platformAnnouncement?: string
  abnormalLoginEnabled?: boolean
  newUserAgentEnabled?: boolean
  batchRegisterIpLimitPerDay?: number
  storageWarnPercent?: number
}

export interface PlatformSecurityOverview {
  openAlertCount: number
  abnormalLoginOpenCount: number
  batchRegisterOpenCount: number
  newUserAgentOpenCount: number
}

export interface PlatformSecurityAlertEvent {
  id: number
  alertType: string
  alertTypeLabel?: string
  severity: string
  userId?: number
  userEmail?: string
  tenantId?: number
  clientIp?: string
  userAgent?: string
  message: string
  metricValue?: number
  threshold?: number
  status: string
  ackedBy?: number
  ackedAt?: string
  createdAt?: string
}

export interface PlatformSecurityAlertEventPage {
  list: PlatformSecurityAlertEvent[]
  total: number
  page: number
  pageSize: number
}

export interface PlatformApiAlertEvent {
  id: number
  alertType: string
  severity: string
  tenantId?: number
  tenantName?: string
  message: string
  metricValue?: number
  threshold?: number
  status: string
  ackedBy?: number
  ackedAt?: string
  createdAt?: string
}

export interface PlatformApiAlertEventPage {
  list: PlatformApiAlertEvent[]
  total: number
  page: number
  pageSize: number
}

export interface PlatformModelCatalogItem {
  id: number
  providerCode: string
  modelName: string
  displayName?: string
  modelType?: string
  inputPricePer1k?: number
  outputPricePer1k?: number
  currency?: string
  enabled?: boolean
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface PlatformModelCatalogPage {
  list: PlatformModelCatalogItem[]
  total: number
  page: number
  pageSize: number
}

export interface PlatformApiAlert {
  type: string
  severity: string
  tenantId?: number
  tenantName?: string
  message: string
  metricValue?: number
  threshold?: number
}

export interface PlatformApiMonitor {
  totalCallsToday: number
  totalCallsLastHour: number
  hourlyCallsThreshold: number
  trafficSpikeMultiplier: number
  alerts: PlatformApiAlert[]
  topTenantsLastHour: PlatformTenantUsage[]
  trafficSpikes: Array<{
    tenantId: number
    tenantName?: string
    todayCalls: number
    avgDailyCalls: number
    spikeRatio?: number
  }>
}

export interface PlatformTenantCreatePayload {
  tenantName: string
  planType?: string
  ownerEmail: string
  ownerPassword?: string
  generatePassword?: boolean
  sendInviteEmail?: boolean
  ownerNickname?: string
  contactName?: string
  contactEmail?: string
  contactPhone?: string
}

export interface PlatformTenantCreateResult {
  tenant: PlatformTenant
  ownerId: number
  ownerEmail: string
  generatedPassword?: string
  inviteEmailSent: boolean
}

export interface PlatformOnboardingTemplate {
  planType: string
  planTypeLabel: string
  maxMembers?: number
  maxAgents?: number
  maxKnowledge?: number
  maxStorageMb?: number
  monthlyTokenQuota?: number
}

export interface PlatformOwnerPasswordResetResult {
  ownerId: number
  ownerEmail: string
  generatedPassword?: string
  inviteEmailSent: boolean
}

export function fetchPlatformTenants(params: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<ApiResult<PlatformTenantPage>>('/v1/platform/tenants', { params })
}

export function fetchPlatformTenant(id: number) {
  return request.get<ApiResult<PlatformTenant>>(`/v1/platform/tenants/${id}`)
}

export function fetchPlatformTenantDetail(id: number) {
  return request.get<ApiResult<PlatformTenantDetail>>(`/v1/platform/tenants/${id}/detail`)
}

export function fetchOnboardingTemplates() {
  return request.get<ApiResult<PlatformOnboardingTemplate[]>>('/v1/platform/onboarding/templates')
}

export function createPlatformTenant(data: PlatformTenantCreatePayload) {
  return request.post<ApiResult<PlatformTenantCreateResult>>('/v1/platform/tenants', data)
}

export function resetTenantOwnerPassword(
  tenantId: number,
  data: { newPassword?: string; generatePassword?: boolean; sendInviteEmail?: boolean },
) {
  return request.post<ApiResult<PlatformOwnerPasswordResetResult>>(
    `/v1/platform/tenants/${tenantId}/owner/reset-password`,
    data,
  )
}

export function updatePlatformTenant(id: number, data: Record<string, unknown>) {
  return request.put<ApiResult<PlatformTenant>>(`/v1/platform/tenants/${id}`, data)
}

export function deletePlatformTenant(id: number) {
  return request.delete<ApiResult<void>>(`/v1/platform/tenants/${id}`)
}

export function fetchPlatformStats() {
  return request.get<ApiResult<PlatformGlobalStats>>('/v1/platform/stats')
}

export function fetchPlatformDashboardOverview() {
  return request.get<ApiResult<PlatformDashboardOverview>>('/v1/platform/dashboard/overview')
}

export function fetchPlatformBillingOverview(month?: string) {
  return request.get<ApiResult<PlatformBillingOverview>>('/v1/platform/billing/overview', {
    params: month ? { month } : undefined,
  })
}

export async function downloadPlatformBillingExport(month?: string) {
  const response = await request.get('/v1/platform/billing/export', {
    params: month ? { month } : undefined,
    responseType: 'blob',
  })
  return response.data as Blob
}

export function fetchPlatformApiAlertEvents(params: {
  page?: number
  pageSize?: number
  status?: string
}) {
  return request.get<ApiResult<PlatformApiAlertEventPage>>('/v1/platform/api-monitor/alerts', { params })
}

export function acknowledgePlatformApiAlert(id: number) {
  return request.post<ApiResult<PlatformApiAlertEvent>>(`/v1/platform/api-monitor/alerts/${id}/ack`)
}

export function fetchPlatformModelCatalog(params: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<ApiResult<PlatformModelCatalogPage>>('/v1/platform/models/catalog', { params })
}

export function createPlatformModelCatalog(data: Record<string, unknown>) {
  return request.post<ApiResult<PlatformModelCatalogItem>>('/v1/platform/models/catalog', data)
}

export function updatePlatformModelCatalog(id: number, data: Record<string, unknown>) {
  return request.put<ApiResult<PlatformModelCatalogItem>>(`/v1/platform/models/catalog/${id}`, data)
}

export function deletePlatformModelCatalog(id: number) {
  return request.delete<ApiResult<void>>(`/v1/platform/models/catalog/${id}`)
}

export function fetchPlatformModelOverview() {
  return request.get<ApiResult<PlatformModelOverview>>('/v1/platform/models/overview')
}

export function fetchPlatformApiMonitor() {
  return request.get<ApiResult<PlatformApiMonitor>>('/v1/platform/api-monitor/overview')
}

export function fetchPlatformModelProviders(params: {
  page?: number
  pageSize?: number
  keyword?: string
  tenantId?: number
  providerCode?: string
  enabled?: number
}) {
  return request.get<ApiResult<PlatformModelProviderPage>>('/v1/platform/models/providers', { params })
}

export function updatePlatformModelProvider(id: number, data: { enabled: number }) {
  return request.put<ApiResult<PlatformModelProvider>>(`/v1/platform/models/providers/${id}`, data)
}

export function fetchPlatformUsers(params: {
  page?: number
  pageSize?: number
  keyword?: string
  status?: number
  accountType?: string
}) {
  return request.get<ApiResult<PlatformUserPage>>('/v1/platform/users', { params })
}

export function fetchPlatformUser(id: number) {
  return request.get<ApiResult<PlatformUser>>(`/v1/platform/users/${id}`)
}

export function updatePlatformUser(id: number, data: { status: number }) {
  return request.put<ApiResult<PlatformUser>>(`/v1/platform/users/${id}`, data)
}

export function forceLogoutPlatformUser(id: number) {
  return request.post<ApiResult<void>>(`/v1/platform/users/${id}/logout`)
}

export function deletePlatformUser(id: number) {
  return request.delete<ApiResult<void>>(`/v1/platform/users/${id}`)
}

export function fetchPlatformSettings() {
  return request.get<ApiResult<PlatformSettings>>('/v1/platform/settings')
}

export function updatePlatformSettings(data: {
  registrationEnabled?: boolean
  hourlyCallsThreshold?: number
  trafficSpikeMultiplier?: number
  allowedProviderCodes?: string[]
  maintenanceEnabled?: boolean
  maintenanceMessage?: string
  platformAnnouncement?: string
  abnormalLoginEnabled?: boolean
  newUserAgentEnabled?: boolean
  batchRegisterIpLimitPerDay?: number
  storageWarnPercent?: number
}) {
  return request.put<ApiResult<PlatformSettings>>('/v1/platform/settings', data)
}

export function fetchPlatformSecurityOverview() {
  return request.get<ApiResult<PlatformSecurityOverview>>('/v1/platform/security/overview')
}

export function fetchPlatformSecurityAlerts(params: {
  page?: number
  pageSize?: number
  status?: string
}) {
  return request.get<ApiResult<PlatformSecurityAlertEventPage>>('/v1/platform/security/alerts', { params })
}

export function acknowledgePlatformSecurityAlert(id: number) {
  return request.post<ApiResult<PlatformSecurityAlertEvent>>(`/v1/platform/security/alerts/${id}/ack`)
}

export function fetchPlatformLoginLogs(params: {
  page?: number
  pageSize?: number
  keyword?: string
  startDate?: string
  endDate?: string
}) {
  return request.get<ApiResult<PlatformLoginLogPage>>('/v1/platform/login-logs', { params })
}

export function fetchIpBlacklist(params: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<ApiResult<IpBlacklistPage>>('/v1/platform/ip-blacklist', { params })
}

export function createIpBlacklist(data: { ipAddress: string; reason?: string; expireAt?: string }) {
  return request.post<ApiResult<IpBlacklistItem>>('/v1/platform/ip-blacklist', data)
}

export function updateIpBlacklist(id: number, data: { reason?: string; status: number; expireAt?: string }) {
  return request.put<ApiResult<IpBlacklistItem>>(`/v1/platform/ip-blacklist/${id}`, data)
}

export function deleteIpBlacklist(id: number) {
  return request.delete<ApiResult<void>>(`/v1/platform/ip-blacklist/${id}`)
}
