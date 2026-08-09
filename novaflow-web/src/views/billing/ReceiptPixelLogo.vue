<template>
  <canvas
    ref="canvasRef"
    :width="canvasWidth"
    :height="canvasHeight"
    :style="{ width: displayWidth + 'px', height: displayHeight + 'px' }"
    class="pixel-logo"
    aria-label="NovaFlow AI"
  />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { LOGO_ICON_SRC } from '@/constants/brand'

const props = withDefaults(defineProps<{
  pixelSize?: number
  sampleSize?: number
}>(), {
  pixelSize: 3,
  sampleSize: 24,
})

const canvasRef = ref<HTMLCanvasElement | null>(null)
const canvasWidth = props.sampleSize * props.pixelSize
const canvasHeight = props.sampleSize * props.pixelSize
const displayWidth = canvasWidth
const displayHeight = canvasHeight

function renderPixelLogo() {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => {
    const tmp = document.createElement('canvas')
    tmp.width = props.sampleSize
    tmp.height = props.sampleSize
    const tctx = tmp.getContext('2d')
    if (!tctx) return

    tctx.clearRect(0, 0, props.sampleSize, props.sampleSize)
    tctx.drawImage(img, 0, 0, props.sampleSize, props.sampleSize)
    const { data } = tctx.getImageData(0, 0, props.sampleSize, props.sampleSize)

    ctx.clearRect(0, 0, canvasWidth, canvasHeight)
    ctx.fillStyle = '#111'

    for (let y = 0; y < props.sampleSize; y += 1) {
      for (let x = 0; x < props.sampleSize; x += 1) {
        const i = (y * props.sampleSize + x) * 4
        const alpha = data[i + 3]
        const luminance = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2]
        if (alpha > 40 && luminance < 235) {
          ctx.fillRect(x * props.pixelSize, y * props.pixelSize, props.pixelSize, props.pixelSize)
        }
      }
    }
  }
  img.src = LOGO_ICON_SRC
}

function getCanvas() {
  return canvasRef.value
}

onMounted(renderPixelLogo)

defineExpose({ getCanvas, renderPixelLogo })
</script>

<style scoped>
.pixel-logo {
  display: block;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
}
</style>
