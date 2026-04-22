import { ref, onUnmounted } from 'vue'
import { markEffectPlayed } from '../api/index.js'

const SSE_URL = '/api/effects/stream'

const effectQueue = ref([])

let eventSource = null

export function useVideoEffect() {
  const connectSSE = () => {
    if (eventSource) return

    eventSource = new EventSource(SSE_URL)

    eventSource.addEventListener('effect', (event) => {
      try {
        const data = JSON.parse(event.data)
        effectQueue.value.push(data)
      } catch { /* ignore malformed events */ }
    })

    eventSource.onerror = () => {
      eventSource?.close()
      eventSource = null
      setTimeout(connectSSE, 5000)
    }
  }

  const consumeEffect = () => {
    if (effectQueue.value.length === 0) return null
    return effectQueue.value.shift()
  }

  const notifyPlayed = async (effectId) => {
    try {
      await markEffectPlayed(effectId)
    } catch { /* silent */ }
  }

  const disconnectSSE = () => {
    eventSource?.close()
    eventSource = null
  }

  onUnmounted(disconnectSSE)

  return {
    effectQueue,
    connectSSE,
    consumeEffect,
    notifyPlayed,
    disconnectSSE
  }
}
