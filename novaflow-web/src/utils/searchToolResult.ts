export interface SearchResultItem {
  title: string
  url?: string
  snippet?: string
}

export interface ParsedSearchToolResult {
  query?: string
  items: SearchResultItem[]
}

export function isSearchToolName(name: string): boolean {
  const normalized = name.trim().toLowerCase()
  return normalized.includes('search') || normalized === 'web_search'
}

export function parseSearchToolArgs(args?: string): string | undefined {
  if (!args?.trim()) {
    return undefined
  }
  try {
    const parsed = JSON.parse(args) as Record<string, unknown>
    const candidate = parsed.query ?? parsed.q ?? parsed.wd ?? parsed.keyword ?? parsed.input
    return candidate != null ? String(candidate).trim() : undefined
  } catch {
    return undefined
  }
}

export function parseSearchToolResult(result?: string): ParsedSearchToolResult {
  if (!result?.trim()) {
    return { items: [] }
  }

  const headerMatch = result.match(/搜索[「"](.+?)[」"]的前\s*(\d+)\s*条结果/)
  const query = headerMatch?.[1]?.trim()
  const items: SearchResultItem[] = []
  const body = result.replace(/^[\s\S]*?条结果：\s*/m, '').trim()

  for (const block of body.split(/\n\n+/)) {
    const lines = block
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)
    if (!lines.length) {
      continue
    }
    const titleMatch = lines[0].match(/^\d+\.\s*(.+)$/)
    if (!titleMatch?.[1]) {
      continue
    }
    let url: string | undefined
    let snippet: string | undefined
    for (let index = 1; index < lines.length; index += 1) {
      const linkMatch = lines[index].match(/^链接:\s*(.+)$/)
      const snippetMatch = lines[index].match(/^摘要:\s*(.+)$/)
      if (linkMatch?.[1]) {
        url = linkMatch[1].trim()
      }
      if (snippetMatch?.[1]) {
        snippet = snippetMatch[1].trim()
      }
    }
    items.push({
      title: titleMatch[1].trim(),
      url,
      snippet,
    })
  }

  return { query, items }
}

export function displayHost(url?: string): string {
  if (!url) {
    return ''
  }
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return url
  }
}

export function faviconUrl(url?: string): string {
  const host = displayHost(url)
  if (!host) {
    return ''
  }
  return `https://www.google.com/s2/favicons?domain=${encodeURIComponent(host)}&sz=32`
}

export function uniqueFaviconHosts(items: SearchResultItem[], limit = 8): Array<{ host: string; url?: string }> {
  const seen = new Set<string>()
  const result: Array<{ host: string; url?: string }> = []
  for (const item of items) {
    const host = displayHost(item.url)
    if (!host || seen.has(host)) {
      continue
    }
    seen.add(host)
    result.push({ host, url: item.url })
    if (result.length >= limit) {
      break
    }
  }
  return result
}

export function toSearchResultItems(sources?: Array<{ title?: string; url?: string; snippet?: string }>): SearchResultItem[] {
  if (!sources?.length) {
    return []
  }
  return sources
    .filter((item) => item.title?.trim() || item.url?.trim())
    .map((item) => ({
      title: item.title?.trim() || displayHost(item.url) || '网页',
      url: item.url?.trim(),
      snippet: item.snippet?.trim(),
    }))
}

export function extractSearchItemsFromContent(content: string): SearchResultItem[] {
  if (!content.trim()) {
    return []
  }
  const items: SearchResultItem[] = []
  const seen = new Set<string>()
  const markdownLinkPattern = /\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g
  let match: RegExpExecArray | null
  while ((match = markdownLinkPattern.exec(content)) !== null) {
    const title = match[1]?.trim()
    const url = match[2]?.trim()
    if (!url || seen.has(url)) {
      continue
    }
    seen.add(url)
    items.push({ title: title || displayHost(url) || '网页', url })
  }
  return items
}
