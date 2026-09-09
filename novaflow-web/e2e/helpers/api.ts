import type { APIRequestContext } from '@playwright/test'
import { ADMIN_EMAIL, ADMIN_PASSWORD } from './auth'

const apiRoot = (process.env.NOVAFLOW_API_URL ?? '').replace(/\/$/, '')

function endpoint(path: string) {
  return apiRoot ? `${apiRoot}${path}` : path
}

type ApiPage<T> = {
  code: number
  data: { list: T[]; total: number }
}

type IdRow = { id: number }

type ApplicationRow = {
  id: number
  appName: string
  description?: string
  agentIds?: number[]
  knowledgeBaseIds?: number[]
}

export async function loginApi(request: APIRequestContext) {
  const login = await request.post(endpoint('/api/v1/auth/login'), {
    data: { email: ADMIN_EMAIL, password: ADMIN_PASSWORD },
  })
  if (!login.ok()) {
    throw new Error(`login failed: HTTP ${login.status()}`)
  }
  const body = await login.json()
  return { token: body.data.token as string, auth: { Authorization: body.data.token as string } }
}

async function deletePaged(
  request: APIRequestContext,
  auth: Record<string, string>,
  path: string,
  keyword: string,
) {
  for (let round = 0; round < 20; round++) {
    const res = await request.get(endpoint(`${path}?page=1&pageSize=100&keyword=${encodeURIComponent(keyword)}`), {
      headers: auth,
    })
    if (!res.ok()) break
    const body = (await res.json()) as ApiPage<IdRow>
    const rows = body.data?.list ?? []
    if (rows.length === 0) break
    let deleted = 0
    for (const row of rows) {
      const del = await request.delete(endpoint(`${path}/${row.id}`), { headers: auth })
      if (del.ok()) deleted++
    }
    if (deleted === 0) break
  }
}

async function unbindAndDeleteE2EApplications(
  request: APIRequestContext,
  auth: Record<string, string>,
) {
  for (let round = 0; round < 20; round++) {
    const res = await request.get(endpoint('/api/v1/applications?page=1&pageSize=100&keyword=E2E-'), {
      headers: auth,
    })
    if (!res.ok()) break
    const body = (await res.json()) as ApiPage<ApplicationRow>
    const rows = body.data?.list ?? []
    if (rows.length === 0) break

    let progressed = false
    for (const row of rows) {
      const detailRes = await request.get(endpoint(`/api/v1/applications/${row.id}`), { headers: auth })
      if (!detailRes.ok()) continue
      const detailBody = await detailRes.json()
      const app = detailBody.data as ApplicationRow
      const agentIds = app.agentIds ?? []

      if (agentIds.length > 0) {
        const unbind = await request.put(endpoint(`/api/v1/applications/${row.id}`), {
          headers: auth,
          data: {
            appName: app.appName,
            description: app.description ?? '',
            agentIds: [],
            knowledgeBaseIds: app.knowledgeBaseIds ?? [],
            defaultAgentId: null,
          },
        })
        if (unbind.ok()) progressed = true
      }

      const del = await request.delete(endpoint(`/api/v1/applications/${row.id}`), { headers: auth })
      if (del.ok()) progressed = true
    }

    if (!progressed) break
  }
}

/** 释放 E2E 用例遗留资源，避免 Agent 触达套餐 500 上限 */
export async function cleanupE2ETestResources(request: APIRequestContext) {
  const { auth } = await loginApi(request)
  await unbindAndDeleteE2EApplications(request, auth)
  await deletePaged(request, auth, '/api/v1/agents', 'E2E-')
  await unbindAndDeleteE2EApplications(request, auth)
}
