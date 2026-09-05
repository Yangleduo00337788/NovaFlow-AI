import { expect, test } from '@playwright/test'

const XSS_MARKDOWN = [
  'SAFE_XSS_MARKER',
  '<script>window.__nfXss=1</script>',
  '<img src=x onerror="window.__nfXss=1">',
  '<svg onload="window.__nfXss=1">',
  '[click](javascript:alert(1))',
].join('\n')

test.describe('XSS 浏览器实测', () => {
  test('应用内 renderMarkdown 消毒后不执行脚本', async ({ page }) => {
    const dialogs: string[] = []
    page.on('dialog', async (dialog) => {
      dialogs.push(dialog.message())
      await dialog.dismiss()
    })

    await page.goto('/about')
    await expect(page.locator('body')).toBeVisible()

    const result = await page.evaluate(async (md) => {
      const mod = await import('/src/utils/markdown.ts')
      const html = mod.renderMarkdown(md)
      const host = document.createElement('main')
      host.className = 'markdown-body'
      host.setAttribute('data-testid', 'xss-sink')
      host.innerHTML = html
      document.body.appendChild(host)
      return {
        html,
        xss: (window as unknown as { __nfXss?: number }).__nfXss ?? null,
      }
    }, XSS_MARKDOWN)

    await expect(page.getByTestId('xss-sink')).toContainText('SAFE_XSS_MARKER')
    expect(result.xss, 'inline script / onerror must not execute').toBeNull()
    expect(dialogs, 'no javascript: alert dialogs').toEqual([])
    expect(result.html.toLowerCase()).not.toContain('<script')
    expect(result.html.toLowerCase()).not.toMatch(/onerror\s*=/)
    expect(result.html.toLowerCase()).not.toMatch(/href\s*=\s*["']?\s*javascript:/)
  })

  test('门户 assistant-content 类名下 renderMarkdown 不执行脚本', async ({ page }) => {
    const dialogs: string[] = []
    page.on('dialog', async (dialog) => {
      dialogs.push(dialog.message())
      await dialog.dismiss()
    })

    await page.goto('/about')
    await expect(page.locator('body')).toBeVisible()

    const result = await page.evaluate(async (md) => {
      const mod = await import('/src/utils/markdown.ts')
      const html = mod.renderMarkdown(md)
      const host = document.createElement('div')
      host.className = 'assistant-content markdown-body'
      host.setAttribute('data-testid', 'portal-xss-sink')
      host.innerHTML = html
      document.body.appendChild(host)
      return {
        html,
        xss: (window as unknown as { __nfXss?: number }).__nfXss ?? null,
      }
    }, XSS_MARKDOWN)

    await expect(page.getByTestId('portal-xss-sink')).toContainText('SAFE_XSS_MARKER')
    expect(result.xss).toBeNull()
    expect(dialogs).toEqual([])
    expect(result.html.toLowerCase()).not.toContain('<script')
  })
})
