import { getDefaultHome } from '@/config/access'

export function getDefaultHomePath(roleCode: string, accountType?: string | null): string {
  return getDefaultHome(accountType, roleCode)
}
