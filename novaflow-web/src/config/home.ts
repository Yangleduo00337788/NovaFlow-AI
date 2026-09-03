import { getDefaultHomeByRole } from '@/config/access'

export function getDefaultHomePath(roleCode: string): string {
  return getDefaultHomeByRole(roleCode)
}
