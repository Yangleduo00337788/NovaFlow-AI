<template>
  <div class="receipt-seal" aria-hidden="true">
    <svg class="seal-fx" width="0" height="0" aria-hidden="true">
      <defs>
        <filter id="seal-ink-rough" x="-20%" y="-20%" width="140%" height="140%">
          <feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="3" seed="8" result="noise" />
          <feDisplacementMap in="SourceGraphic" in2="noise" scale="1.8" xChannelSelector="R" yChannelSelector="G" />
        </filter>
        <filter id="seal-ink-grain" x="-15%" y="-15%" width="130%" height="130%">
          <feTurbulence type="fractalNoise" baseFrequency="0.65" numOctaves="4" seed="17" result="grain" />
          <feColorMatrix
            in="grain"
            type="matrix"
            values="0 0 0 0 0.72
                    0 0 0 0 0.08
                    0 0 0 0 0.08
                    0 0 0 1 0"
            result="redGrain"
          />
          <feBlend in="SourceGraphic" in2="redGrain" mode="multiply" />
        </filter>
        <filter id="seal-ink-bleed" x="-25%" y="-25%" width="150%" height="150%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="0.35" result="blur" />
          <feTurbulence type="turbulence" baseFrequency="0.08" numOctaves="2" seed="3" result="maskNoise" />
          <feDisplacementMap in="blur" in2="maskNoise" scale="2.5" xChannelSelector="R" yChannelSelector="G" />
        </filter>
        <filter id="seal-edge-wear" x="-10%" y="-10%" width="120%" height="120%">
          <feTurbulence type="fractalNoise" baseFrequency="0.12" numOctaves="2" seed="21" result="edgeNoise" />
          <feComponentTransfer in="edgeNoise" result="edgeAlpha">
            <feFuncA type="discrete" tableValues="0 0 0 1 1 1 1 0 0 1" />
          </feComponentTransfer>
          <feComposite in="SourceGraphic" in2="edgeAlpha" operator="in" />
        </filter>
      </defs>
    </svg>

    <div class="seal-stack">
      <div class="seal-ghost" />
      <div class="seal-body">
        <div class="seal-frame">
          <span class="seal-edge seal-edge-outer" />
          <span class="seal-edge seal-edge-inner" />
        </div>

        <div class="seal-glyphs">
          <span class="glyph glyph-tl">乐</span>
          <span class="glyph glyph-tr">养</span>
          <span class="glyph glyph-bl">印</span>
          <span class="glyph glyph-br">多</span>
        </div>

        <div class="seal-patches">
          <span v-for="n in 6" :key="n" class="seal-patch" :class="`patch-${n}`" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fontsapi.zeoseven.com/236/main/result.css');

.receipt-seal {
  --seal-red: #b42323;
  --seal-red-deep: #8f1a1a;
  --seal-red-light: rgba(180, 35, 35, 0.42);
  position: absolute;
  right: 16px;
  bottom: 88px;
  width: 86px;
  height: 86px;
  transform: rotate(-11deg);
  pointer-events: none;
  mix-blend-mode: multiply;
}

.seal-fx {
  position: absolute;
  overflow: hidden;
}

.seal-stack {
  position: relative;
  width: 100%;
  height: 100%;
}

.seal-ghost {
  position: absolute;
  inset: 4px;
  border-radius: 2px;
  background: radial-gradient(ellipse 70% 60% at 52% 48%, rgba(180, 35, 35, 0.22), transparent 72%);
  filter: blur(1.2px);
  opacity: 0.85;
}

.seal-body {
  position: relative;
  width: 100%;
  height: 100%;
  padding: 5px;
  filter: url(#seal-ink-grain);
}

.seal-frame {
  position: absolute;
  inset: 0;
}

.seal-edge {
  position: absolute;
  inset: 0;
  border: 2.6px solid var(--seal-red);
  opacity: 0.9;
  filter: url(#seal-ink-rough) url(#seal-edge-wear);
}

.seal-edge-inner {
  inset: 7px;
  border-width: 1.4px;
  opacity: 0.78;
}

.seal-glyphs {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  width: 100%;
  height: 100%;
  padding: 10px 8px 8px;
  box-sizing: border-box;
}

.glyph {
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: '峄山碑篆体', 'FZS3JW', 'STXinwei', serif;
  font-size: 26px;
  line-height: 1;
  color: var(--seal-red-deep);
  text-shadow:
    0 0 0.4px rgba(143, 26, 26, 0.95),
    0.3px 0.2px 0 rgba(143, 26, 26, 0.55),
    -0.2px 0.1px 0 rgba(143, 26, 26, 0.35);
  filter: url(#seal-ink-bleed) url(#seal-ink-rough);
  opacity: 0.92;
}

.glyph-tr,
.glyph-br {
  padding-left: 1px;
}

.glyph-tl {
  opacity: 0.88;
}

.glyph-bl {
  opacity: 0.84;
}

.glyph-br {
  opacity: 0.9;
}

.seal-patches {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.seal-patch {
  position: absolute;
  border-radius: 50%;
  background: var(--seal-red-light);
  filter: blur(0.6px);
  mix-blend-mode: multiply;
}

.patch-1 {
  width: 14px;
  height: 10px;
  top: 18%;
  left: 62%;
  opacity: 0.55;
}

.patch-2 {
  width: 11px;
  height: 8px;
  bottom: 24%;
  left: 20%;
  opacity: 0.45;
}

.patch-3 {
  width: 9px;
  height: 12px;
  top: 42%;
  right: 8%;
  opacity: 0.38;
}

.patch-4 {
  width: 16px;
  height: 7px;
  bottom: 12%;
  right: 28%;
  opacity: 0.42;
}

.patch-5 {
  width: 7px;
  height: 7px;
  top: 8%;
  left: 28%;
  opacity: 0.35;
}

.patch-6 {
  width: 10px;
  height: 6px;
  top: 58%;
  left: 46%;
  opacity: 0.3;
}
</style>
