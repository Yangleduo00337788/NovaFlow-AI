/** 与后端 AccountTypes 保持一致 */
export const AccountTypes = {
  TENANT: 'tenant',
  PLATFORM: 'platform',
} as const

export type AccountType = (typeof AccountTypes)[keyof typeof AccountTypes]

export function isPlatformAccount(accountType?: string | null): boolean {
  return accountType === AccountTypes.PLATFORM
}
