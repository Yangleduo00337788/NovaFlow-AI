import type { SearchResultItem } from '@/utils/searchToolResult'

const CIRCLED_NUMBERS = [
  '①', '②', '③', '④', '⑤', '⑥', '⑦', '⑧', '⑨', '⑩',
  '⑪', '⑫', '⑬', '⑭', '⑮', '⑯', '⑰', '⑱', '⑲', '⑳',
]

function escapeAttr(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function isSafeHttpUrl(url: string): boolean {
  try {
    const parsed = new URL(url)
    return parsed.protocol === 'http:' || parsed.protocol === 'https:'
  } catch {
    return false
  }
}

function buildCitation(index: number, source?: SearchResultItem): string {
  const label = CIRCLED_NUMBERS[index] || `[${index + 1}]`
  if (!source?.url || !isSafeHttpUrl(source.url)) {
    return `<span class="search-citation" title="${escapeAttr(source?.title || source?.url || '')}">${label}</span>`
  }
  const href = escapeAttr(source.url)
  return `<a class="search-citation" href="${href}" target="_blank" rel="noopener noreferrer" title="${escapeAttr(source.title)}">${label}</a>`
}

export function injectSearchCitations(html: string, sources: SearchResultItem[]): string {
  if (!sources.length || !html.trim()) {
    return html
  }

  let result = html
  result = result.replace(/【(\d{1,2})】/g, (match, num) => {
    const index = Number(num) - 1
    if (index < 0 || index >= sources.length) {
      return match
    }
    return buildCitation(index, sources[index])
  })
  result = result.replace(/\[(\d{1,2})\]/g, (match, num) => {
    const index = Number(num) - 1
    if (index < 0 || index >= sources.length) {
      return match
    }
    return buildCitation(index, sources[index])
  })
  return result
}
