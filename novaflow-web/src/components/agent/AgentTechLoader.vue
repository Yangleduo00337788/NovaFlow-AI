<template>
  <div
    class="tech-loader"
    :class="[
      sizeClass,
      { 'tech-loader--paused': paused },
    ]"
    :aria-label="ariaLabel"
  >
    <span class="tech-loader__ring tech-loader__ring--outer" />
    <span class="tech-loader__ring tech-loader__ring--inner" />
    <span class="tech-loader__core">
      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path
          d="M12 3L20 8V16L12 21L4 16V8L12 3Z"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linejoin="round"
        />
        <circle cx="12" cy="12" r="2.5" fill="currentColor" />
      </svg>
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    size?: 'xs' | 'md' | 'send'
    paused?: boolean
    ariaLabel?: string
  }>(),
  {
    size: 'md',
    paused: false,
    ariaLabel: '加载中',
  },
)

const sizeClass = computed(() => {
  if (props.size === 'xs') return 'tech-loader--xs'
  if (props.size === 'send') return 'tech-loader--send'
  return ''
})
</script>

<style scoped>
.tech-loader {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tech-loader--xs {
  width: 20px;
  height: 20px;
}

.tech-loader--xs .tech-loader__ring--inner {
  inset: 4px;
}

.tech-loader--xs .tech-loader__core {
  width: 10px;
  height: 10px;
}

.tech-loader--send {
  width: 18px;
  height: 18px;
}

.tech-loader--send .tech-loader__ring--inner {
  inset: 3px;
}

.tech-loader--send .tech-loader__core {
  width: 9px;
  height: 9px;
}

.tech-loader--paused .tech-loader__ring--outer,
.tech-loader--paused .tech-loader__ring--inner,
.tech-loader--paused .tech-loader__core {
  animation-play-state: paused;
}

.tech-loader--paused .tech-loader__ring--outer,
.tech-loader--paused .tech-loader__ring--inner {
  opacity: 0.9;
}

.tech-loader__ring {
  position: absolute;
  border-radius: 50%;
  border: 2px solid transparent;
}

.tech-loader__ring--outer {
  inset: 0;
  border-top-color: #69b1ff;
  border-right-color: rgba(54, 207, 201, 0.85);
  box-shadow: 0 0 14px rgba(22, 119, 255, 0.25);
  animation: tech-spin 1.1s linear infinite;
}

.tech-loader__ring--inner {
  inset: 7px;
  border-bottom-color: #1677ff;
  border-left-color: rgba(105, 177, 255, 0.7);
  animation: tech-spin 0.75s linear infinite reverse;
}

.tech-loader__core {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  color: #1677ff;
  filter: drop-shadow(0 0 6px rgba(22, 119, 255, 0.55));
  animation: tech-pulse 1.4s ease-in-out infinite;
}

.tech-loader__core svg {
  width: 100%;
  height: 100%;
}

@keyframes tech-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes tech-pulse {
  0%,
  100% {
    opacity: 0.75;
    transform: scale(0.92);
  }

  50% {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
