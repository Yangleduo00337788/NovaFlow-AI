import type { AgentToolDefinition } from '@/api/agent'

export interface ToolFormItem extends AgentToolDefinition {
  headerRows: Array<{ key: string; value: string }>
  inputSchemaJson: string
}

export function createToolFormItem(): ToolFormItem {
  return {
    name: '',
    description: '',
    method: 'GET',
    url: '',
    bodyTemplate: '',
    headerRows: [],
    inputSchemaJson: '',
  }
}

export function toToolFormItems(items?: AgentToolDefinition[]): ToolFormItem[] {
  return (items || [])
    .filter((tool) => tool.toolType !== 'baidu_search')
    .map((tool) => ({
      name: tool.name || '',
      description: tool.description || '',
      method: tool.method || 'GET',
      url: tool.url || '',
      bodyTemplate: tool.bodyTemplate || '',
      headerRows: Object.entries(tool.headers || {}).map(([key, value]) => ({ key, value })),
      inputSchemaJson: tool.inputSchema ? JSON.stringify(tool.inputSchema, null, 2) : '',
    }))
}

export function serializeToolFormItems(items: ToolFormItem[]): AgentToolDefinition[] {
  return items
    .filter((tool) => tool.name?.trim() && tool.url?.trim())
    .map((tool) => {
      const headers: Record<string, string> = {}
      for (const row of tool.headerRows) {
        if (row.key?.trim() && row.value != null) {
          headers[row.key.trim()] = row.value
        }
      }
      let inputSchema: Record<string, unknown> | undefined
      if (tool.inputSchemaJson?.trim()) {
        inputSchema = JSON.parse(tool.inputSchemaJson) as Record<string, unknown>
      }
      return {
        name: tool.name.trim(),
        description: tool.description?.trim(),
        method: tool.method || 'GET',
        url: tool.url?.trim(),
        bodyTemplate: tool.bodyTemplate?.trim() || undefined,
        headers: Object.keys(headers).length ? headers : undefined,
        inputSchema,
      }
    })
}
