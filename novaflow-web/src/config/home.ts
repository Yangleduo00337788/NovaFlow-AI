import { getDefaultHome } from '@/config/access'

export function getDefaultHomePath(
  roleCode: string,
  accountType?: string | null,
  canAccess?: (path: string) => boolean,
): string {
  return getDefaultHome(accountType, roleCode, canAccess)
}
