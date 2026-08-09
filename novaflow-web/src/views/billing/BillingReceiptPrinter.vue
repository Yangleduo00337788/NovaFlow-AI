<template>
  <Teleport to="body">
    <Transition name="receipt-fade">
      <div v-if="open" class="receipt-overlay" @click.self="handleClose">
        <div class="receipt-stage">
          <div class="stage-inner">
            <button type="button" class="overlay-close" aria-label="关闭" @click="handleClose">
              <span class="close-icon">×</span>
            </button>

            <div
              class="printer-unit"
              :class="{
                printing: phase === 'printing',
                done: phase === 'done',
              }"
            >
              <div class="printer-scene">
                <div class="printer-desk-shadow" />

                <div class="printer-shell">
                  <div class="printer-back" />

                  <div class="printer-top-cap">
                    <div class="cap-highlight" />
                    <span class="cap-brand">NovaFlow</span>
                    <span class="cap-model">TP-80</span>
                    <div class="printer-led" :class="{ on: phase === 'printing' }" />
                  </div>

                  <div class="printer-body">
                    <div class="printer-side left">
                      <div class="side-shine" />
                    </div>

                    <div class="printer-front">
                      <div class="front-bezel" />
                      <div class="front-label">RECEIPT PRINTER</div>

                      <div class="control-deck">
                        <div class="screen-bezel">
                          <div class="printer-screen">
                            <span v-if="phase === 'idle'">READY</span>
                            <span v-else-if="phase === 'printing'" class="blink">PRINTING...</span>
                            <span v-else>DONE ✓</span>
                          </div>
                        </div>
                        <button
                          type="button"
                          class="printer-btn"
                          :disabled="phase !== 'idle'"
                          @click="startPrint"
                        >
                          <span class="printer-btn-shadow" />
                          <span class="printer-btn-cap" />
                          <span class="printer-btn-face">打 印</span>
                        </button>
                      </div>

                      <div class="vent-grille">
                        <span v-for="n in 16" :key="n" />
                      </div>

                      <div class="paper-slot">
                        <div class="slot-roller"><span class="roller-axis" /></div>
                        <div class="slot-opening" />
                        <div class="slot-roller"><span class="roller-axis" /></div>
                      </div>
                    </div>

                    <div class="printer-side right">
                      <div class="side-shine" />
                    </div>
                  </div>

                  <div class="printer-chin">
                    <div class="chin-lip" />
                  </div>

                  <div class="printer-feet">
                    <span v-for="n in 4" :key="n" class="foot" />
                  </div>
                </div>
              </div>

              <div
                class="paper-outlet"
                :class="{ active: phase !== 'idle' }"
                :style="outletStyle"
              >
                <div ref="receiptRef" class="receipt-paper">
                  <ReceiptBody
                    :overview="overview"
                    :month="month"
                    :total-records="totalRecords"
                    :receipt-no="receiptNo"
                    :printed-at="printedAt"
                  />
                </div>
              </div>
            </div>

            <div v-if="phase === 'done'" class="receipt-actions">
              <a-button @click="handleClose">关闭</a-button>
              <a-button type="primary" :loading="downloading" @click="handleDownload">
                <DownloadOutlined />
                下载小票
              </a-button>
            </div>

            <p v-if="phase === 'idle'" class="printer-hint">点击打印机上的「打印」按钮出票</p>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import { toPng } from 'html-to-image'
import type { BillingOverview } from '@/api/billing'
import { PrinterSound } from '@/utils/printerSound'
import ReceiptBody from './ReceiptBody.vue'

const props = defineProps<{
  open: boolean
  overview: BillingOverview
  month: string
  totalRecords: number
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()

const receiptRef = ref<HTMLElement | null>(null)
const receiptHeight = ref(0)
const downloading = ref(false)
const receiptNo = ref('')
const phase = ref<'idle' | 'printing' | 'done'>('idle')
const printProgress = ref(0)

let printTimer: ReturnType<typeof setInterval> | null = null
let finishTimer: ReturnType<typeof setTimeout> | null = null
const printerSound = new PrinterSound()

const printedAt = computed(() => dayjs().format('YYYY-MM-DD HH:mm:ss'))

const outletStyle = computed(() => {
  if (phase.value === 'idle') {
    return { height: '0px' }
  }
  const height = Math.round((printProgress.value / 100) * receiptHeight.value)
  return { height: `${height}px` }
})

function generateReceiptNo() {
  const rand = Math.floor(Math.random() * 9000 + 1000)
  receiptNo.value = `NF${props.month.replace('-', '')}${rand}`
}

async function measureReceipt() {
  await nextTick()
  const el = receiptRef.value
  const outlet = el?.parentElement as HTMLElement | null
  if (!el || !outlet) return

  const prev = {
    height: outlet.style.height,
    overflow: outlet.style.overflow,
    visibility: outlet.style.visibility,
    position: outlet.style.position,
  }

  outlet.style.height = 'auto'
  outlet.style.overflow = 'visible'
  outlet.style.visibility = 'hidden'
  outlet.style.position = 'absolute'
  outlet.style.left = '-9999px'
  receiptHeight.value = el.offsetHeight

  outlet.style.height = prev.height
  outlet.style.overflow = prev.overflow
  outlet.style.visibility = prev.visibility
  outlet.style.position = prev.position
  outlet.style.left = ''
}

function clearTimers() {
  if (printTimer) {
    clearInterval(printTimer)
    printTimer = null
  }
  if (finishTimer) {
    clearTimeout(finishTimer)
    finishTimer = null
  }
}

function resetState() {
  clearTimers()
  printerSound.stop()
  phase.value = 'idle'
  printProgress.value = 0
}

async function startPrint() {
  if (phase.value !== 'idle') return
  await measureReceipt()
  if (!receiptHeight.value) return

  phase.value = 'printing'
  printProgress.value = 0
  void printerSound.start()

  printTimer = setInterval(() => {
    printProgress.value = Math.min(100, printProgress.value + 2)
    if (printProgress.value >= 100) {
      clearTimers()
      printerSound.stop()
      finishTimer = setTimeout(() => {
        phase.value = 'done'
      }, 350)
    }
  }, 45)
}

function handleClose() {
  resetState()
  emit('update:open', false)
}

function cloneReceiptNode(source: HTMLElement) {
  const clone = source.cloneNode(true) as HTMLElement
  const sourceCanvases = source.querySelectorAll('canvas')
  const cloneCanvases = clone.querySelectorAll('canvas')
  sourceCanvases.forEach((canvas, index) => {
    const target = cloneCanvases[index] as HTMLCanvasElement | undefined
    if (!target) return
    const ctx = target.getContext('2d')
    if (ctx) {
      ctx.drawImage(canvas, 0, 0)
    }
  })
  return clone
}

async function handleDownload() {
  const source = receiptRef.value?.querySelector('.receipt-body') as HTMLElement | null
  if (!source) {
    message.error('小票内容不存在')
    return
  }

  downloading.value = true
  const sandbox = document.createElement('div')
  sandbox.style.cssText = 'position:fixed;left:-9999px;top:0;z-index:-1;pointer-events:none;'

  try {
    await document.fonts.load('26px "峄山碑篆体"')
    await document.fonts.ready

    const clone = cloneReceiptNode(source)
    sandbox.appendChild(clone)
    document.body.appendChild(sandbox)

    const dataUrl = await toPng(clone, {
      pixelRatio: 2,
      backgroundColor: '#fffef8',
      cacheBust: true,
    })

    const link = document.createElement('a')
    link.href = dataUrl
    link.download = `novaflow-receipt-${props.month}.png`
    link.click()
    message.success('小票已下载')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '下载失败')
  } finally {
    if (sandbox.parentNode) {
      sandbox.parentNode.removeChild(sandbox)
    }
    downloading.value = false
  }
}

onBeforeUnmount(() => {
  printerSound.stop()
})

watch(
  () => props.open,
  async (value) => {
    if (value) {
      resetState()
      generateReceiptNo()
      await measureReceipt()
    } else {
      resetState()
    }
  },
)
</script>

<style scoped>
.receipt-overlay {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.58);
  backdrop-filter: blur(5px);
  padding: 24px;
}

.receipt-stage {
  width: 100%;
  max-height: 94vh;
  display: flex;
  justify-content: center;
  overflow: auto;
}

.stage-inner {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding-top: 12px;
}

.overlay-close {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 5;
  width: 34px;
  height: 34px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}

.overlay-close:hover {
  color: #334155;
  background: #f8fafc;
}

.close-icon {
  display: block;
  font-size: 22px;
  font-weight: 400;
  line-height: 1;
  transform: translateY(-1px);
}

.printer-unit {
  position: relative;
  width: min(500px, 94vw);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.printer-scene {
  position: relative;
  width: 100%;
  perspective: 1100px;
  perspective-origin: 50% 35%;
}

.printer-desk-shadow {
  position: absolute;
  left: 6%;
  right: 6%;
  bottom: 8px;
  height: 36px;
  background: radial-gradient(ellipse at center, rgba(0, 0, 0, 0.42) 0%, transparent 72%);
  transform: rotateX(72deg) scaleY(0.55);
  pointer-events: none;
  z-index: 0;
}

.printer-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  transform: rotateX(7deg);
  transform-style: preserve-3d;
  filter: drop-shadow(0 22px 36px rgba(0, 0, 0, 0.38));
}

.printer-back {
  position: absolute;
  top: 18px;
  left: 22px;
  right: 22px;
  height: calc(100% - 28px);
  background: linear-gradient(180deg, #2a3544 0%, #1e2836 100%);
  border-radius: 10px;
  transform: translateZ(-18px) translateY(6px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.35);
  z-index: 0;
}

.printer-top-cap {
  position: relative;
  z-index: 3;
  height: 34px;
  margin: 0 14px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.22) 0%, transparent 28%),
    linear-gradient(180deg, #6d8096 0%, #4f6278 45%, #3d4f63 100%);
  border-radius: 16px 16px 6px 6px;
  border: 1px solid #7d91a8;
  border-bottom: 2px solid #2f3d4d;
  box-shadow:
    inset 0 2px 0 rgba(255, 255, 255, 0.28),
    inset 0 -3px 6px rgba(0, 0, 0, 0.18),
    0 6px 0 #2f3d4d;
  transform: translateZ(8px);
}

.cap-highlight {
  position: absolute;
  top: 5px;
  left: 24px;
  right: 24px;
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.35), transparent);
  pointer-events: none;
}

.cap-brand {
  font-size: 12px;
  font-weight: 800;
  color: #f8fafc;
  letter-spacing: 0.6px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
}

.cap-model {
  font-size: 10px;
  color: #b6c4d4;
  font-weight: 600;
}

.printer-led {
  margin-left: auto;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #64748b, #334155);
  box-shadow:
    inset 0 -2px 3px rgba(0, 0, 0, 0.5),
    0 1px 0 rgba(255, 255, 255, 0.15);
}

.printer-led.on {
  background: radial-gradient(circle at 35% 30%, #86efac, #22c55e);
  box-shadow:
    0 0 14px #4ade80,
    0 0 4px #86efac,
    inset 0 0 5px rgba(255, 255, 255, 0.45);
  animation: led-blink 0.7s ease-in-out infinite;
}

.printer-body {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: stretch;
  margin: -2px 2px 0;
  transform: translateZ(4px);
}

.printer-side {
  position: relative;
  width: 22px;
  flex-shrink: 0;
  background:
    linear-gradient(90deg, rgba(0, 0, 0, 0.25), transparent 40%),
    linear-gradient(180deg, #4a5d72 0%, #354556 55%, #2a3644 100%);
  box-shadow:
    inset 0 0 12px rgba(0, 0, 0, 0.35),
    inset 2px 0 4px rgba(255, 255, 255, 0.06);
}

.printer-side.left {
  border-radius: 10px 0 0 10px;
  transform: rotateY(14deg);
  transform-origin: right center;
  margin-right: -2px;
}

.printer-side.right {
  border-radius: 0 10px 10px 0;
  transform: rotateY(-14deg);
  transform-origin: left center;
  margin-left: -2px;
  background:
    linear-gradient(270deg, rgba(0, 0, 0, 0.25), transparent 40%),
    linear-gradient(180deg, #4a5d72 0%, #354556 55%, #2a3644 100%);
}

.side-shine {
  position: absolute;
  top: 12%;
  bottom: 20%;
  width: 3px;
  border-radius: 999px;
  background: linear-gradient(180deg, transparent, rgba(255, 255, 255, 0.12), transparent);
}

.printer-side.left .side-shine {
  right: 4px;
}

.printer-side.right .side-shine {
  left: 4px;
}

.printer-front {
  position: relative;
  flex: 1;
  padding: 16px 22px 12px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.1) 0%, transparent 18%),
    linear-gradient(180deg, #5d7186 0%, #4a5d72 38%, #3b4c5e 100%);
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow:
    inset 0 3px 0 rgba(255, 255, 255, 0.14),
    inset 0 -6px 12px rgba(0, 0, 0, 0.15);
}

.front-bezel {
  position: absolute;
  inset: 6px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.12);
  pointer-events: none;
}

.front-label {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2.5px;
  color: #a8b8c8;
  margin-bottom: 12px;
  text-shadow: 0 1px 0 rgba(0, 0, 0, 0.35);
}

.control-deck {
  display: flex;
  align-items: center;
  gap: 16px;
}

.screen-bezel {
  flex: 1;
  padding: 4px;
  border-radius: 10px;
  background: linear-gradient(180deg, #1a2330 0%, #0f172a 100%);
  box-shadow:
    inset 0 3px 8px rgba(0, 0, 0, 0.65),
    0 1px 0 rgba(255, 255, 255, 0.08);
}

.printer-screen {
  padding: 10px 14px;
  background:
    radial-gradient(ellipse at 30% 20%, rgba(74, 222, 128, 0.08), transparent 55%),
    #060b12;
  border-radius: 6px;
  border: 1px solid #1e293b;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  font-weight: 700;
  color: #4ade80;
  letter-spacing: 1px;
  box-shadow: inset 0 4px 10px rgba(0, 0, 0, 0.7);
  text-shadow: 0 0 10px rgba(74, 222, 128, 0.55);
}

.printer-screen .blink {
  animation: text-blink 0.55s step-end infinite;
}

.printer-btn {
  position: relative;
  border: none;
  padding: 0;
  background: transparent;
  cursor: pointer;
  width: 80px;
  height: 52px;
  transform: translateZ(6px);
}

.printer-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.printer-btn-shadow {
  position: absolute;
  left: 6px;
  right: 6px;
  bottom: -4px;
  height: 10px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.35);
  filter: blur(4px);
}

.printer-btn-cap {
  position: absolute;
  inset: 0;
  border-radius: 10px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, #e2e8f0 45%, #94a3b8 100%);
  border: 2px solid #cbd5e1;
  box-shadow:
    inset 0 2px 0 rgba(255, 255, 255, 0.9),
    inset 0 -3px 6px rgba(0, 0, 0, 0.12),
    0 5px 0 #64748b,
    0 8px 16px rgba(0, 0, 0, 0.28);
  transition: transform 0.12s, box-shadow 0.12s;
}

.printer-btn-face {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 14px;
  font-weight: 800;
  color: #1e293b;
  letter-spacing: 3px;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.6);
}

.printer-btn:not(:disabled):active .printer-btn-cap,
.printer-unit.printing .printer-btn-cap {
  transform: translateY(4px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.5),
    0 1px 0 #64748b,
    0 3px 6px rgba(0, 0, 0, 0.2);
}

.vent-grille {
  display: flex;
  gap: 5px;
  margin: 14px 0 12px;
  justify-content: center;
  padding: 6px 12px;
  border-radius: 6px;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.12), rgba(0, 0, 0, 0.22));
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.25);
}

.vent-grille span {
  width: 3px;
  height: 16px;
  border-radius: 1px;
  background: linear-gradient(180deg, #3a4858 0%, #1a2330 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 1px 0 rgba(255, 255, 255, 0.04);
}

.paper-slot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: linear-gradient(180deg, #141c28 0%, #060b12 100%);
  border-radius: 6px;
  border: 1px solid #334155;
  box-shadow:
    inset 0 4px 10px rgba(0, 0, 0, 0.75),
    0 1px 0 rgba(255, 255, 255, 0.06);
}

.slot-roller {
  position: relative;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: radial-gradient(circle at 32% 28%, #94a3b8, #475569 55%, #334155);
  box-shadow:
    inset 0 -2px 4px rgba(0, 0, 0, 0.5),
    0 1px 2px rgba(255, 255, 255, 0.15);
}

.roller-axis {
  position: absolute;
  inset: 4px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.35);
  background: radial-gradient(circle at 40% 35%, #64748b, #1e293b);
}

.printer-unit.printing .slot-roller {
  animation: roller-spin 0.28s linear infinite;
}

.slot-opening {
  flex: 1;
  height: 7px;
  background: #000;
  border-radius: 3px;
  box-shadow:
    inset 0 3px 6px rgba(0, 0, 0, 0.95),
    0 1px 0 rgba(255, 255, 255, 0.05);
}

.printer-chin {
  position: relative;
  z-index: 2;
  height: 14px;
  margin: 0 24px;
  background: linear-gradient(180deg, #3f5164 0%, #2f3f50 100%);
  border-radius: 0 0 10px 10px;
  border: 1px solid #556678;
  border-top: none;
  box-shadow:
    0 6px 12px rgba(0, 0, 0, 0.28),
    inset 0 -2px 4px rgba(0, 0, 0, 0.2);
  transform: translateZ(2px);
}

.chin-lip {
  position: absolute;
  left: 12%;
  right: 12%;
  bottom: 2px;
  height: 3px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.25);
}

.printer-feet {
  display: flex;
  justify-content: space-between;
  padding: 0 36px;
  margin-top: -2px;
  transform: translateZ(1px);
}

.foot {
  width: 28px;
  height: 8px;
  border-radius: 50%;
  background: radial-gradient(ellipse at center, #1e293b 0%, #0f172a 100%);
  box-shadow:
    0 4px 6px rgba(0, 0, 0, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.paper-outlet {
  position: relative;
  z-index: 0;
  width: min(420px, 84vw);
  margin-top: -4px;
  overflow: hidden;
  height: 0;
  transition: height 0.04s linear;
  filter: drop-shadow(0 8px 16px rgba(0, 0, 0, 0.12));
}

.paper-outlet.active {
  transition: height 0.04s linear;
}

.receipt-paper {
  width: 100%;
}

.printer-unit.done .receipt-paper {
  animation: paper-settle 0.4s ease-out;
}

.printer-hint {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  text-align: center;
}

.receipt-actions {
  display: flex;
  gap: 12px;
}

.receipt-fade-enter-active,
.receipt-fade-leave-active {
  transition: opacity 0.25s ease;
}

.receipt-fade-enter-from,
.receipt-fade-leave-to {
  opacity: 0;
}

@keyframes led-blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.35;
  }
}

@keyframes text-blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.25;
  }
}

@keyframes roller-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes paper-settle {
  0% {
    transform: translateY(-2px);
  }
  55% {
    transform: translateY(1px);
  }
  100% {
    transform: translateY(0);
  }
}
</style>
