/**
 * 仿真热敏打印机音效（Web Audio API，无需外部音频文件）
 */
export class PrinterSound {
  private ctx: AudioContext | null = null
  private motorNode: AudioBufferSourceNode | null = null
  private motorGain: GainNode | null = null
  private tickTimer: ReturnType<typeof setInterval> | null = null

  async start() {
    this.stop()

    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    if (!AudioCtx) return

    this.ctx = new AudioCtx()
    if (this.ctx.state === 'suspended') {
      await this.ctx.resume()
    }

    const sampleRate = this.ctx.sampleRate
    const duration = 2
    const buffer = this.ctx.createBuffer(1, sampleRate * duration, sampleRate)
    const data = buffer.getChannelData(0)

    for (let i = 0; i < data.length; i += 1) {
      const t = i / sampleRate
      const noise = (Math.random() * 2 - 1) * 0.22
      const hum = Math.sin(t * 120 * Math.PI * 2) * 0.04
      const flutter = Math.sin(t * 7 * Math.PI * 2) * 0.03
      data[i] = noise + hum + flutter
    }

    this.motorNode = this.ctx.createBufferSource()
    this.motorNode.buffer = buffer
    this.motorNode.loop = true

    const bandpass = this.ctx.createBiquadFilter()
    bandpass.type = 'bandpass'
    bandpass.frequency.value = 900
    bandpass.Q.value = 0.8

    this.motorGain = this.ctx.createGain()
    this.motorGain.gain.value = 0.12

    this.motorNode.connect(bandpass)
    bandpass.connect(this.motorGain)
    this.motorGain.connect(this.ctx.destination)
    this.motorNode.start()

    this.tickTimer = setInterval(() => this.playTick(), 70)
  }

  stop() {
    if (this.tickTimer) {
      clearInterval(this.tickTimer)
      this.tickTimer = null
    }

    if (this.motorNode && this.motorGain && this.ctx) {
      const now = this.ctx.currentTime
      this.motorGain.gain.setValueAtTime(this.motorGain.gain.value, now)
      this.motorGain.gain.exponentialRampToValueAtTime(0.001, now + 0.15)
      this.motorNode.stop(now + 0.16)
    }

    this.motorNode = null
    this.motorGain = null

    if (this.ctx) {
      const ctx = this.ctx
      setTimeout(() => {
        void ctx.close()
      }, 200)
      this.ctx = null
    }
  }

  private playTick() {
    if (!this.ctx) return

    const osc = this.ctx.createOscillator()
    const gain = this.ctx.createGain()
    const filter = this.ctx.createBiquadFilter()

    osc.type = 'square'
    osc.frequency.value = 1800 + Math.random() * 400

    filter.type = 'highpass'
    filter.frequency.value = 1200

    const now = this.ctx.currentTime
    gain.gain.setValueAtTime(0.0001, now)
    gain.gain.exponentialRampToValueAtTime(0.06 + Math.random() * 0.03, now + 0.004)
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.025)

    osc.connect(filter)
    filter.connect(gain)
    gain.connect(this.ctx.destination)

    osc.start(now)
    osc.stop(now + 0.03)
  }
}
