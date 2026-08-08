const CURRENCY_SYMBOLS: Record<string, string> = {
  CNY: '¥',
  USD: '$',
}

export function currencySymbol(currency?: string) {
  if (!currency) return '¥'
  return CURRENCY_SYMBOLS[currency.toUpperCase()] || currency
}

export function formatMoney(value?: number | string | null, currency = 'CNY') {
  if (value == null || value === '') return '-'
  const num = typeof value === 'string' ? Number(value) : value
  if (Number.isNaN(num)) return String(value)
  if (num === 0) return '-'
  const symbol = currencySymbol(currency)
  const digits = num > 0 && num < 0.01 ? 4 : 2
  return `${symbol}${num.toFixed(digits)}`
}

export function formatCostSummaries(
  summaries?: Array<{ currency: string; symbol: string; amount: string }>,
  fallback = '¥0.00',
) {
  if (!summaries || summaries.length === 0) return fallback
  return summaries.map((item) => `${item.symbol}${item.amount}`).join(' + ')
}
