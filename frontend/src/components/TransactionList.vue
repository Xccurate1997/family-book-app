<template>
  <div class="transaction-list">
    <div v-if="!transactions.length" class="empty-state">
      <el-empty description="本月暂无记录，点击「记一笔」开始记账" />
    </div>

    <template v-for="[date, txs] in grouped" :key="date">
      <div class="date-header">
        <span class="date-text">{{ formatDate(date) }}</span>
        <span class="daily-net" :class="dailyNetClass(txs)">
          {{ dailyNetText(txs) }}
        </span>
      </div>

      <div
        v-for="tx in txs"
        :key="tx.id"
        class="tx-item"
      >
        <span class="tx-icon">{{ tx.category?.icon }}</span>
        <div class="tx-info">
          <span class="tx-category">{{ tx.category?.name }}</span>
          <span v-if="tx.description" class="tx-desc">{{ tx.description }}</span>
        </div>
        <span class="tx-amount" :class="tx.type === 'INCOME' ? 'income' : 'expense'">
          {{ tx.type === 'INCOME' ? '+' : '-' }}¥{{ fmt(tx.amount) }}
        </span>
        <div class="tx-actions">
          <el-button text size="small" :icon="Edit" @click="$emit('edit', tx)" />
          <el-button text size="small" :icon="Delete" type="danger" @click="$emit('delete', tx)" />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Edit, Delete } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const props = defineProps({ transactions: Array })
defineEmits(['edit', 'delete'])

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']

const grouped = computed(() => {
  const map = {}
  for (const tx of props.transactions) {
    const d = tx.transactionDate
    if (!map[d]) map[d] = []
    map[d].push(tx)
  }
  return Object.entries(map).sort((a, b) => b[0].localeCompare(a[0]))
})

const fmt = (n) =>
  Number(n || 0)
    .toFixed(2)
    .replace(/\B(?=(\d{3})+(?!\d))/g, ',')

const formatDate = (dateStr) => {
  const d = dayjs(dateStr)
  return `${d.format('MM月DD日')} 周${WEEKDAYS[d.day()]}`
}

const dailyNet = (txs) =>
  txs.reduce((sum, tx) => {
    return tx.type === 'INCOME' ? sum + Number(tx.amount) : sum - Number(tx.amount)
  }, 0)

const dailyNetText = (txs) => {
  const n = dailyNet(txs)
  return (n >= 0 ? '+' : '') + '¥' + Math.abs(n).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const dailyNetClass = (txs) => (dailyNet(txs) >= 0 ? 'net-positive' : 'net-negative')
</script>

<style scoped>
.date-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 4px 6px;
  font-size: 13px;
  color: #888;
}

.net-positive { color: #43a047; }
.net-negative { color: #e53935; }

.tx-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s;
}

.tx-item:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.tx-icon {
  font-size: 24px;
  width: 36px;
  text-align: center;
  flex-shrink: 0;
}

.tx-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.tx-category {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.tx-desc {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tx-amount {
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}

.income { color: #43a047; }
.expense { color: #e53935; }

.tx-actions {
  display: flex;
  gap: 0;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.tx-item:hover .tx-actions {
  opacity: 1;
}

.empty-state {
  padding: 60px 0;
  text-align: center;
}
</style>
