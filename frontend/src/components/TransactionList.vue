<template>
  <div class="transaction-list">
    <div v-if="!transactions.length" class="empty-state">
      <div v-if="emptyStateImage" class="custom-empty">
        <img :src="emptyStateImage" class="custom-empty-img" />
        <p class="custom-empty-text">本月暂无记录，点击「记一笔」开始记账</p>
      </div>
      <el-empty v-else description="本月暂无记录，点击「记一笔」开始记账" />
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
        :class="{ 'has-bg': getTxBgImage(tx) }"
        :style="getTxBgStyle(tx)"
      >
        <!-- 背景图叠加层，保证文字可读性 -->
        <div v-if="getTxBgImage(tx)" class="tx-bg-overlay"></div>
        <span class="tx-icon">{{ tx.category?.icon }}</span>
        <div class="tx-info">
          <span class="tx-category">{{ tx.category?.name }}</span>
          <span v-if="tx.description" class="tx-desc">{{ tx.description }}</span>
        </div>
        <span class="tx-amount" :class="tx.type === 'INCOME' ? 'income' : 'expense'">
          {{ tx.type === 'INCOME' ? '+' : '-' }}¥{{ fmt(tx.amount) }}
        </span>
        <span v-if="tx.moodEmoji" class="tx-mood" :style="{ fontSize: emojiStyle.text }">
          <span v-if="tx.moodEmoji.type === 'TEXT'">{{ tx.moodEmoji.content }}</span>
          <img v-else :src="`/api/emoticon-images/${tx.moodEmoji.content}`" class="mood-img" :style="{ width: emojiStyle.img + 'px', height: emojiStyle.img + 'px' }" />
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
import { useEmojiSize } from '../composables/useEmojiSize.js'

const { sizeStyle } = useEmojiSize()
const emojiStyle = computed(() => sizeStyle())

const props = defineProps({
  transactions: Array,
  emptyStateImage: { type: String, default: null },
  incomeBgImage: { type: String, default: null },
  expenseBgImage: { type: String, default: null }
})

const getTxBgImage = (tx) => {
  return tx.type === 'INCOME' ? props.incomeBgImage : props.expenseBgImage
}

const getTxBgStyle = (tx) => {
  const bgImage = getTxBgImage(tx)
  if (!bgImage) return {}
  return {
    backgroundImage: `url(${bgImage})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    backgroundRepeat: 'no-repeat'
  }
}
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
  position: relative;
  overflow: hidden;
}

.tx-item:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

/* 有背景图时的叠加层 */
.tx-bg-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.75);
  z-index: 0;
  pointer-events: none;
}

/* 有背景图时，内容元素需要在叠加层之上 */
.tx-item.has-bg > *:not(.tx-bg-overlay) {
  position: relative;
  z-index: 1;
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

.tx-mood {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  line-height: 1;
}

.mood-img {
  object-fit: contain;
}

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

/* 触摸设备始终显示操作按钮 */
@media (hover: none) {
  .tx-actions {
    opacity: 1;
  }
}

@media (max-width: 640px) {
  .tx-item {
    gap: 8px;
    padding: 10px 12px;
  }
  .tx-icon {
    font-size: 20px;
    width: 28px;
  }
  .tx-category {
    font-size: 13px;
  }
  .tx-amount {
    font-size: 14px;
  }
  .custom-empty-img {
    width: 120px;
    height: 120px;
  }
}

.empty-state {
  padding: 60px 0;
  text-align: center;
}
.custom-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.custom-empty-img {
  width: 180px;
  height: 180px;
  object-fit: contain;
  opacity: 0.85;
}
.custom-empty-text {
  font-size: 13px;
  color: #999;
  margin: 0;
}
</style>
