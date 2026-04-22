<template>
  <Transition name="effect-fade" @after-leave="onFadeComplete">
    <div v-if="visible" class="effect-overlay" :style="{ opacity: overlayOpacity }">
      <!-- 自定义视频模式：居中悬浮播放 -->
      <div v-if="currentVideoUrl" class="video-stage">
        <video
          ref="videoRef"
          class="effect-video"
          :src="currentVideoUrl"
          autoplay
          muted
          playsinline
          @timeupdate="onTimeUpdate"
          @ended="startFadeOut"
          @error="onVideoError"
        />
      </div>

      <!-- Canvas 动画模式：全屏透明绘制 -->
      <canvas
        v-else
        ref="canvasRef"
        class="effect-canvas"
      />
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch, nextTick, onUnmounted } from 'vue'
import { useVideoEffect } from '../composables/useVideoEffect.js'

const { effectQueue, consumeEffect, notifyPlayed } = useVideoEffect()

const visible = ref(false)
const fading = ref(false)
const currentVideoUrl = ref(null)
const currentEffectType = ref(null)
const videoRef = ref()
const canvasRef = ref()
const overlayOpacity = ref(1)

let currentEffectId = null
let canvasAnimationId = null
let fadeOutTimer = null
let fadeOutAnimationId = null

const ANIMATION_DURATION = 4000
const FADE_OUT_LEAD_TIME = 1500

watch(effectQueue, () => {
  if (visible.value || fading.value) return
  tryPlayNext()
}, { deep: true })

function tryPlayNext() {
  const effect = consumeEffect()
  if (!effect) return

  currentEffectId = effect.id
  currentEffectType.value = effect.type || 'FIREWORKS'
  currentVideoUrl.value = effect.videoUrl || null
  visible.value = true

  nextTick(() => {
    if (!currentVideoUrl.value) {
      startCanvasAnimation(currentEffectType.value)
    }
  })
}

function beginGradualFadeOut() {
  if (fadeOutAnimationId) return
  const startTime = performance.now()
  function tick(now) {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / FADE_OUT_LEAD_TIME, 1)
    overlayOpacity.value = 1 - progress
    if (progress < 1) {
      fadeOutAnimationId = requestAnimationFrame(tick)
    } else {
      fadeOutAnimationId = null
      startFadeOut()
    }
  }
  fadeOutAnimationId = requestAnimationFrame(tick)
}

function stopGradualFadeOut() {
  if (fadeOutAnimationId) {
    cancelAnimationFrame(fadeOutAnimationId)
    fadeOutAnimationId = null
  }
  if (fadeOutTimer) {
    clearTimeout(fadeOutTimer)
    fadeOutTimer = null
  }
}

function startFadeOut() {
  fading.value = true
  visible.value = false
}

function onFadeComplete() {
  fading.value = false
  cleanup()

  if (currentEffectId) {
    notifyPlayed(currentEffectId)
    currentEffectId = null
  }

  nextTick(tryPlayNext)
}

function onTimeUpdate() {
  const video = videoRef.value
  if (!video || fadeOutAnimationId) return
  const remaining = video.duration - video.currentTime
  if (remaining <= FADE_OUT_LEAD_TIME / 1000) {
    beginGradualFadeOut()
  }
}

function onVideoError() {
  currentVideoUrl.value = null
  nextTick(() => startCanvasAnimation('FIREWORKS'))
}

function cleanup() {
  stopCanvasAnimation()
  stopGradualFadeOut()
  currentVideoUrl.value = null
  currentEffectType.value = null
  overlayOpacity.value = 1
}

// ── Canvas 动画引擎 ──────────────────────────────────

function startCanvasAnimation(effectType) {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  switch (effectType) {
    case 'HEARTS':
      runHeartsAnimation(ctx, canvas)
      break
    case 'GOLD_RAIN':
      runGoldRainAnimation(ctx, canvas)
      break
    default:
      runFireworksAnimation(ctx, canvas)
  }

  fadeOutTimer = setTimeout(() => {
    beginGradualFadeOut()
  }, ANIMATION_DURATION - FADE_OUT_LEAD_TIME)
}

function stopCanvasAnimation() {
  if (canvasAnimationId) {
    cancelAnimationFrame(canvasAnimationId)
    canvasAnimationId = null
  }
}

// ── 烟花动画 (FIREWORKS) ─────────────────────────────

class Particle {
  constructor(x, y, color) {
    this.x = x
    this.y = y
    this.color = color
    const angle = Math.random() * Math.PI * 2
    const speed = Math.random() * 5 + 2
    this.velocityX = Math.cos(angle) * speed
    this.velocityY = Math.sin(angle) * speed
    this.alpha = 1
    this.decay = Math.random() * 0.015 + 0.012
    this.size = Math.random() * 3 + 1.5
    this.gravity = 0.05
    this.trail = []
  }

  update() {
    this.trail.push({ x: this.x, y: this.y, alpha: this.alpha })
    if (this.trail.length > 5) this.trail.shift()
    this.velocityY += this.gravity
    this.x += this.velocityX
    this.y += this.velocityY
    this.alpha -= this.decay
  }

  draw(ctx) {
    for (const point of this.trail) {
      ctx.beginPath()
      ctx.arc(point.x, point.y, this.size * 0.5, 0, Math.PI * 2)
      ctx.fillStyle = this.color.replace('1)', `${point.alpha * 0.3})`)
      ctx.fill()
    }
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fillStyle = this.color.replace('1)', `${this.alpha})`)
    ctx.shadowBlur = 8
    ctx.shadowColor = this.color.replace('1)', '0.6)')
    ctx.fill()
    ctx.shadowBlur = 0
  }
}

class Rocket {
  constructor(canvasWidth, canvasHeight) {
    this.x = canvasWidth * (0.2 + Math.random() * 0.6)
    this.y = canvasHeight
    this.targetY = canvasHeight * (0.15 + Math.random() * 0.35)
    this.speed = 4 + Math.random() * 3
    this.color = randomColor()
    this.trail = []
  }

  update() {
    this.trail.push({ x: this.x, y: this.y })
    if (this.trail.length > 8) this.trail.shift()
    this.y -= this.speed
    return this.y <= this.targetY
  }

  draw(ctx) {
    for (let i = 0; i < this.trail.length; i++) {
      const point = this.trail[i]
      ctx.beginPath()
      ctx.arc(point.x, point.y, 2, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(255, 200, 100, ${(i / this.trail.length) * 0.4})`
      ctx.fill()
    }
    ctx.beginPath()
    ctx.arc(this.x, this.y, 3, 0, Math.PI * 2)
    ctx.fillStyle = '#ffe0a0'
    ctx.shadowBlur = 12
    ctx.shadowColor = '#ffcc66'
    ctx.fill()
    ctx.shadowBlur = 0
  }
}

const FIREWORK_PALETTE = [
  'rgba(255, 80, 80, 1)', 'rgba(255, 180, 50, 1)',
  'rgba(80, 255, 120, 1)', 'rgba(80, 180, 255, 1)',
  'rgba(200, 100, 255, 1)', 'rgba(255, 100, 200, 1)',
  'rgba(255, 255, 100, 1)', 'rgba(100, 255, 255, 1)'
]

function randomColor() {
  return FIREWORK_PALETTE[Math.floor(Math.random() * FIREWORK_PALETTE.length)]
}

function runFireworksAnimation(ctx, canvas) {
  let rockets = []
  let particles = []
  let lastRocketTime = 0

  function animate(timestamp) {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    if (timestamp - lastRocketTime > 300 + Math.random() * 500) {
      rockets.push(new Rocket(canvas.width, canvas.height))
      lastRocketTime = timestamp
    }

    rockets = rockets.filter(rocket => {
      const reached = rocket.update()
      if (reached) {
        const count = 60 + Math.floor(Math.random() * 40)
        for (let i = 0; i < count; i++) {
          particles.push(new Particle(rocket.x, rocket.y, rocket.color))
        }
        return false
      }
      rocket.draw(ctx)
      return true
    })

    particles = particles.filter(particle => {
      particle.update()
      if (particle.alpha <= 0) return false
      particle.draw(ctx)
      return true
    })

    canvasAnimationId = requestAnimationFrame(animate)
  }

  canvasAnimationId = requestAnimationFrame(animate)
}

// ── 爱心动画 (HEARTS) ────────────────────────────────

class Heart {
  constructor(canvasWidth, canvasHeight) {
    this.x = Math.random() * canvasWidth
    this.y = canvasHeight + 20
    this.size = 12 + Math.random() * 20
    this.speed = 1.5 + Math.random() * 2.5
    this.wobbleSpeed = 0.02 + Math.random() * 0.03
    this.wobbleAmount = 30 + Math.random() * 40
    this.phase = Math.random() * Math.PI * 2
    this.alpha = 0.7 + Math.random() * 0.3
    this.rotation = (Math.random() - 0.5) * 0.3
    this.hue = 340 + Math.random() * 30
    this.startX = this.x
    this.time = 0
  }

  update() {
    this.time += this.wobbleSpeed
    this.y -= this.speed
    this.x = this.startX + Math.sin(this.time + this.phase) * this.wobbleAmount
    this.alpha -= 0.002
  }

  draw(ctx) {
    ctx.save()
    ctx.translate(this.x, this.y)
    ctx.rotate(this.rotation)
    ctx.globalAlpha = Math.max(0, this.alpha)
    ctx.fillStyle = `hsla(${this.hue}, 80%, 60%, 1)`
    ctx.shadowBlur = 10
    ctx.shadowColor = `hsla(${this.hue}, 80%, 60%, 0.5)`
    drawHeartShape(ctx, this.size)
    ctx.shadowBlur = 0
    ctx.restore()
  }
}

function drawHeartShape(ctx, size) {
  const scale = size / 30
  ctx.beginPath()
  ctx.moveTo(0, -8 * scale)
  ctx.bezierCurveTo(-15 * scale, -25 * scale, -30 * scale, -5 * scale, 0, 15 * scale)
  ctx.bezierCurveTo(30 * scale, -5 * scale, 15 * scale, -25 * scale, 0, -8 * scale)
  ctx.closePath()
  ctx.fill()
}

function runHeartsAnimation(ctx, canvas) {
  let hearts = []
  let lastSpawnTime = 0

  function animate(timestamp) {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    if (timestamp - lastSpawnTime > 80 + Math.random() * 120) {
      hearts.push(new Heart(canvas.width, canvas.height))
      lastSpawnTime = timestamp
    }

    hearts = hearts.filter(heart => {
      heart.update()
      if (heart.alpha <= 0 || heart.y < -50) return false
      heart.draw(ctx)
      return true
    })

    canvasAnimationId = requestAnimationFrame(animate)
  }

  canvasAnimationId = requestAnimationFrame(animate)
}

// ── 金币雨动画 (GOLD_RAIN) ───────────────────────────

class GoldCoin {
  constructor(canvasWidth) {
    this.x = Math.random() * canvasWidth
    this.y = -20 - Math.random() * 100
    this.size = 10 + Math.random() * 14
    this.speed = 2 + Math.random() * 3
    this.wobbleSpeed = 0.03 + Math.random() * 0.04
    this.wobbleAmount = 15 + Math.random() * 25
    this.phase = Math.random() * Math.PI * 2
    this.alpha = 0.8 + Math.random() * 0.2
    this.rotation = Math.random() * Math.PI * 2
    this.rotationSpeed = (Math.random() - 0.5) * 0.1
    this.startX = this.x
    this.time = 0
  }

  update() {
    this.time += this.wobbleSpeed
    this.y += this.speed
    this.x = this.startX + Math.sin(this.time + this.phase) * this.wobbleAmount
    this.rotation += this.rotationSpeed
  }

  draw(ctx) {
    ctx.save()
    ctx.translate(this.x, this.y)
    ctx.rotate(this.rotation)
    ctx.globalAlpha = this.alpha

    // 金币外圈
    ctx.beginPath()
    ctx.arc(0, 0, this.size, 0, Math.PI * 2)
    ctx.fillStyle = '#FFD700'
    ctx.shadowBlur = 8
    ctx.shadowColor = 'rgba(255, 215, 0, 0.6)'
    ctx.fill()
    ctx.shadowBlur = 0

    // 金币内圈
    ctx.beginPath()
    ctx.arc(0, 0, this.size * 0.75, 0, Math.PI * 2)
    ctx.fillStyle = '#FFC107'
    ctx.fill()

    // ¥ 符号
    ctx.fillStyle = '#B8860B'
    ctx.font = `bold ${this.size}px sans-serif`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('¥', 0, 1)

    ctx.restore()
  }
}

function runGoldRainAnimation(ctx, canvas) {
  let coins = []
  let lastSpawnTime = 0

  function animate(timestamp) {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    if (timestamp - lastSpawnTime > 60 + Math.random() * 100) {
      coins.push(new GoldCoin(canvas.width))
      lastSpawnTime = timestamp
    }

    coins = coins.filter(coin => {
      coin.update()
      if (coin.y > canvas.height + 30) return false
      coin.draw(ctx)
      return true
    })

    canvasAnimationId = requestAnimationFrame(animate)
  }

  canvasAnimationId = requestAnimationFrame(animate)
}

onUnmounted(cleanup)
</script>

<style scoped>
.effect-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.video-stage {
  width: 60vw;
  height: 50vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.effect-video {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  pointer-events: none;
  border-radius: 12px;
}

.effect-video::-webkit-media-controls {
  display: none !important;
}

.effect-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.effect-fade-enter-active {
  transition: opacity 0.3s ease;
}

.effect-fade-leave-active {
  transition: opacity 0.3s ease;
}

.effect-fade-enter-from,
.effect-fade-leave-to {
  opacity: 0;
}
</style>
