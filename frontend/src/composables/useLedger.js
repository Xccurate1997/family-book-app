import { ref } from 'vue'
import { getLedgers } from '../api/index.js'

const STORAGE_KEY = 'bookapp_ledger_id'

const ledgers = ref([])
const currentLedgerId = ref(null)

export function useLedger() {
  const load = async () => {
    const res = await getLedgers()
    ledgers.value = res.data
    const saved = localStorage.getItem(STORAGE_KEY)
    const savedId = saved ? Number(saved) : null
    const exists = savedId && ledgers.value.some(l => l.id === savedId)
    currentLedgerId.value = exists ? savedId : (ledgers.value[0]?.id ?? null)
    if (currentLedgerId.value) {
      localStorage.setItem(STORAGE_KEY, String(currentLedgerId.value))
    }
  }

  const select = (id) => {
    currentLedgerId.value = id
    localStorage.setItem(STORAGE_KEY, String(id))
  }

  return { ledgers, currentLedgerId, load, select }
}
