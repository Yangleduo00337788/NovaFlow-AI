<template>
  <div class="receipt-body">
    <div class="receipt-perforation left" />
    <div class="receipt-perforation right" />

    <div class="receipt-inner">
      <div class="corner-ornament top-left">✦</div>
      <div class="corner-ornament top-right">✦</div>

      <div class="receipt-header">
        <ReceiptPixelLogo :pixel-size="3" :sample-size="22" />
        <div class="brand-name">NovaFlow AI</div>
        <div class="receipt-banner">◆ 用量结算小票 ◆</div>
        <div class="receipt-sub">TOKEN USAGE RECEIPT</div>
      </div>

      <div class="receipt-divider"><span>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</span></div>

      <div class="receipt-grid two-col">
        <div class="receipt-row">
          <span>账期</span>
          <span>{{ overview.periodLabel || month }}</span>
        </div>
        <div class="receipt-row">
          <span>套餐</span>
          <span>{{ overview.quota.planTypeLabel || '免费版' }}</span>
        </div>
        <div class="receipt-row">
          <span>出票时间</span>
          <span>{{ printedAt }}</span>
        </div>
        <div class="receipt-row">
          <span>小票编号</span>
          <span>{{ receiptNo }}</span>
        </div>
      </div>

      <div class="receipt-divider ornament"><span>— ✦ 汇 总 ✦ —</span></div>

      <div class="receipt-grid summary-grid">
        <div class="summary-item">
          <span class="label">本月调用</span>
          <span class="value">{{ formatNumber(overview.totalCalls) }} 次</span>
        </div>
        <div class="summary-item">
          <span class="label">本月 Token</span>
          <span class="value">{{ formatNumber(overview.totalTokens) }}</span>
        </div>
        <div class="summary-item highlight">
          <span class="label">预估费用</span>
          <span class="value">{{ overview.totalCostLabel }}</span>
        </div>
        <div class="summary-item muted">
          <span class="label">Token 环比</span>
          <span class="value">{{ overview.tokenChangePercent }}</span>
        </div>
        <div class="summary-item muted">
          <span class="label">调用环比</span>
          <span class="value">{{ overview.callChangePercent }}</span>
        </div>
        <div class="summary-item muted">
          <span class="label">明细条数</span>
          <span class="value">{{ formatNumber(totalRecords) }} 条</span>
        </div>
      </div>

      <div v-if="overview.usageByType.length || overview.topModels.length" class="receipt-columns">
        <div v-if="overview.usageByType.length" class="receipt-col">
          <div class="receipt-divider ornament small"><span>✦ 类型分布</span></div>
          <div
            v-for="item in overview.usageByType"
            :key="item.usageType"
            class="receipt-row compact"
          >
            <span>{{ item.usageTypeLabel }}</span>
            <span>{{ formatNumber(item.tokens) }} T</span>
          </div>
        </div>

        <div v-if="overview.topModels.length" class="receipt-col">
          <div class="receipt-divider ornament small"><span>✦ 模型 TOP</span></div>
          <div
            v-for="(item, index) in overview.topModels.slice(0, 5)"
            :key="item.modelName"
            class="receipt-row compact"
          >
            <span>{{ index + 1 }}. {{ shorten(item.displayName || item.modelName, 12) }}</span>
            <span>{{ formatNumber(item.tokens) }}</span>
          </div>
        </div>
      </div>

      <div class="receipt-row compact footer-row">
        <span>成员席位</span>
        <span>{{ overview.quota.memberCount }} / {{ overview.quota.maxMembers }}</span>
      </div>

      <ReceiptSeal />

      <div class="receipt-footer">
        <div class="footer-line">★ 感谢您的使用 ★</div>
        <div class="footer-note">本小票为系统预估费用，仅供参考</div>
        <div class="pixel-decoration">
          <span v-for="n in 36" :key="n" class="pixel-dot" />
        </div>
        <div class="barcode">
          <span v-for="n in 56" :key="n" :style="{ height: barcodeHeight(n) + 'px' }" />
        </div>
        <div class="footer-code">{{ receiptNo }}</div>
        <div class="footer-site">novaflow.ai</div>
      </div>

      <div class="corner-ornament bottom-left">✦</div>
      <div class="corner-ornament bottom-right">✦</div>
    </div>

    <div class="receipt-tear" />
  </div>
</template>

<script setup lang="ts">
import type { BillingOverview } from '@/api/billing'
import ReceiptPixelLogo from './ReceiptPixelLogo.vue'
import ReceiptSeal from './ReceiptSeal.vue'

const props = defineProps<{
  overview: BillingOverview
  month: string
  totalRecords: number
  receiptNo: string
  printedAt: string
}>()

function formatNumber(value?: number) {
  return value != null ? value.toLocaleString() : '0'
}

function shorten(text: string, max: number) {
  return text.length > max ? `${text.slice(0, max)}…` : text
}

function barcodeHeight(n: number) {
  return 6 + ((n * 7 + props.receiptNo.charCodeAt(n % props.receiptNo.length)) % 16)
}
</script>

<style scoped>
.receipt-body {
  position: relative;
  width: 100%;
  background: #fffef8;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.16);
  background-image: radial-gradient(rgba(0, 0, 0, 0.03) 1px, transparent 1px);
  background-size: 4px 4px;
}

.receipt-inner {
  position: relative;
  padding: 20px 22px 8px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 11px;
  line-height: 1.45;
  color: #1a1a1a;
}

.receipt-perforation {
  position: absolute;
  top: 0;
  bottom: 12px;
  width: 8px;
  pointer-events: none;
  background-image: radial-gradient(circle, #d1d5db 2px, transparent 2px);
  background-size: 8px 14px;
  background-repeat: repeat-y;
}

.receipt-perforation.left {
  left: 4px;
}

.receipt-perforation.right {
  right: 4px;
}

.corner-ornament {
  position: absolute;
  font-size: 10px;
  color: #999;
  line-height: 1;
}

.corner-ornament.top-left {
  top: 8px;
  left: 14px;
}

.corner-ornament.top-right {
  top: 8px;
  right: 14px;
}

.corner-ornament.bottom-left {
  bottom: 18px;
  left: 14px;
}

.corner-ornament.bottom-right {
  bottom: 18px;
  right: 14px;
}

.receipt-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  margin-bottom: 4px;
}

.brand-name {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
}

.receipt-banner {
  font-size: 13px;
  font-weight: 700;
}

.receipt-sub {
  font-size: 10px;
  color: #666;
  letter-spacing: 0.5px;
}

.receipt-divider {
  text-align: center;
  color: #888;
  font-size: 10px;
  margin: 8px 0;
  overflow: hidden;
  white-space: nowrap;
}

.receipt-divider.ornament {
  color: #444;
  font-weight: 600;
}

.receipt-divider.small {
  margin: 4px 0 6px;
  text-align: left;
}

.receipt-grid.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 20px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px 12px;
  margin: 4px 0 8px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 8px;
  border: 1px dashed #ccc;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.02);
}

.summary-item .label {
  font-size: 9px;
  color: #666;
}

.summary-item .value {
  font-size: 11px;
  font-weight: 600;
}

.summary-item.highlight {
  grid-column: span 1;
  border-color: #999;
}

.summary-item.highlight .value {
  font-size: 12px;
}

.summary-item.muted .value {
  font-weight: 500;
  color: #444;
}

.receipt-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin: 6px 0;
  padding-top: 4px;
  border-top: 1px dashed #bbb;
}

.receipt-col {
  min-width: 0;
}

.receipt-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.receipt-row.compact {
  font-size: 10px;
  margin-bottom: 2px;
}

.footer-row {
  margin-top: 8px;
  padding-top: 6px;
  border-top: 1px dashed #bbb;
  font-size: 10px;
}

.receipt-footer {
  margin-top: 10px;
  text-align: center;
}

.footer-line {
  font-size: 11px;
  font-weight: 600;
}

.footer-note {
  margin-top: 3px;
  font-size: 10px;
  color: #666;
}

.pixel-decoration {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 3px;
  margin: 8px auto 6px;
  max-width: 260px;
}

.pixel-dot {
  width: 4px;
  height: 4px;
  background: #111;
  opacity: 0.75;
}

.pixel-dot:nth-child(3n) {
  opacity: 0.35;
}

.barcode {
  display: flex;
  justify-content: center;
  gap: 1px;
  margin: 6px 0 4px;
  height: 22px;
  align-items: flex-end;
}

.barcode span {
  display: block;
  width: 2px;
  background: #111;
}

.footer-code {
  font-size: 10px;
  letter-spacing: 1px;
}

.footer-site {
  margin-top: 3px;
  font-size: 10px;
  color: #666;
}

.receipt-tear {
  height: 10px;
  background:
    linear-gradient(135deg, #fffef8 33.33%, transparent 33.33%) 0 0,
    linear-gradient(225deg, #fffef8 33.33%, transparent 33.33%) 0 0;
  background-size: 12px 10px;
  background-repeat: repeat-x;
}
</style>
