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

function buildCitation(index: number, source?: SearchResultItem): string {
  const label = CIRCLED_NUMBERS[index] || `[${index + 1}]`
  if (!source?.url) {
    return `<span class="search-citation" title="${escapeAttr(source?.title || '')}">${label}</span>`
  }
  const href = source.url.replace(/"/g, '&quot;')
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
