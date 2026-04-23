<template>
  <div class="calendar-root">
    <!-- 月份导航 -->
    <div class="calendar-header">
      <span class="section-label">记账日历</span>
      <div class="month-nav">
        <el-button :icon="ArrowLeft" circle size="small" @click="prevMonth" />
        <span class="month-label">{{ displayMonth }}</span>
        <el-button :icon="ArrowRight" circle size="small" @click="nextMonth" />
      </div>
    </div>

    <!-- 日历网格 -->
    <div class="calendar-grid">
      <!-- 星期头 -->
      <div v-for="w in WEEKDAYS" :key="w" class="weekday-header">{{ w }}</div>

      <!-- 日期格子 -->
      <div
        v-for="(cell, idx) in calendarCells"
        :key="idx"
        class="day-cell"
        :class="{
          empty: !cell.day,
          today: cell.isToday,
          'has-data': cell.day && (cell.income > 0 || cell.expense > 0)
        }"
        :style="cell.day ? { backgroundColor: heatColor(cell.expense) } : {}"
        @click="cell.day && onDayClick(cell)"
      >
        <template v-if="cell.day">
          <div class="day-number">{{ cell.day }}</div>
          <div v-if="cell.income > 0" class="day-income">+{{ shortMoney(cell.income) }}</div>
          <div v-if="cell.expense > 0" class="day-expense">-{{ shortMoney(cell.expense) }}</div>
        </template>
      </div>
    </div>

    <!-- 日期明细抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      direction="rtl"
      :size="windowWidth < 640 ? '85%' : '360px'"
    >
      <div v-if="drawerLoading" class="drawer-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      <template v-else>
        <div v-if="dayTransactions.length === 0" class="drawer-empty">
          <el-empty description="当日无记录" :image-size="60" />
        </div>
        <div v-else class="tx-list">
          <div v-for="tx in dayTransactions" :key="tx.id" class="tx-item">
            <div class="tx-left">
              <span class="tx-icon">{{ tx.category?.icon || '📦' }}</span>
              <div>
                <div class="tx-cat">{{ tx.category?.name || '未分类' }}</div>
                <div v-if="tx.description" class="tx-desc">{{ tx.description }}</div>
              </div>
            </div>
            <div class="tx-right">
              <span class="tx-amount" :class="tx.type === 'INCOME' ? 'green' : 'red'">
                {{ tx.type === 'INCOME' ? '+' : '-' }}{{ Number(tx.amount).toFixed(2) }}
              </span>
              <span v-if="tx.moodEmoji" class="tx-mood" :style="{ fontSize: emojiStyle.text }">
                <span v-if="tx.moodEmoji.type === 'TEXT'">{{ tx.moodEmoji.content }}</span>
                <img v-else :src="`/api/emoticon-images/${tx.moodEmoji.content}`" class="mood-img" :style="{ width: emojiStyle.img + 'px', height: emojiStyle.img + 'px' }" />
              </span>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <el-button type="primary" @click="handleAddForDay">记一笔</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ArrowLeft, ArrowRight, Loading } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getDailyStats, getTransactionsByDate } from '../api/index.js'
import { useEmojiSize } from '../composables/useEmojiSize.js'

const { sizeStyle } = useEmojiSize()
const emojiStyle = computed(() => sizeStyle())

const props = defineProps({
  ledgerId: Number,
  categories: Array
})

const emit = defineEmits(['addTransaction'])

const windowWidth = ref(window.innerWidth)
const onResize = () => { windowWidth.value = window.innerWidth }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']

const currentDate = ref(dayjs().startOf('month'))
const displayMonth = computed(() => currentDate.value.format('YYYY年MM月'))
const dailyData = ref([])
const maxExpense = ref(0)

// Drawer state
const drawerVisible = ref(false)
const drawerLoading = ref(false)
const selectedDate = ref(null)
const dayTransactions = ref([])
const drawerTitle = computed(() =>
  selectedDate.value ? dayjs(selectedDate.value).format('M月D日 dddd') : ''
)

const calendarCells = computed(() => {
  const first = currentDate.value.startOf('month')
  const daysInMonth = currentDate.value.daysInMonth()
  const startWeekday = first.day() // 0=Sunday
  const today = dayjs().format('YYYY-MM-DD')

  const cells = []
  // Fill empty cells before first day
  for (let i = 0; i < startWeekday; i++) {
    cells.push({ day: null })
  }
  // Fill day cells
  for (let d = 1; d <= daysInMonth; d++) {
    const stat = dailyData.value.find(s => s.day === d)
    const dateStr = currentDate.value.date(d).format('YYYY-MM-DD')
    cells.push({
      day: d,
      date: dateStr,
      income: stat ? Number(stat.income) : 0,
      expense: stat ? Number(stat.expense) : 0,
      isToday: dateStr === today
    })
  }
  // Fill remaining to complete 6 rows
  while (cells.length < 42) {
    cells.push({ day: null })
  }
  return cells
})

const heatColor = (expense) => {
  if (!expense || expense <= 0 || maxExpense.value <= 0) return 'transparent'
  const intensity = Math.min(expense / maxExpense.value, 1) * 0.15
  return `rgba(229, 57, 53, ${intensity})`
}

const shortMoney = (v) => {
  if (v >= 10000) return (v / 10000).toFixed(1) + 'w'
  if (v >= 1000) return (v / 1000).toFixed(1) + 'k'
  return v.toFixed(0)
}

const loadData = async () => {
  if (!props.ledgerId) return
  const year = currentDate.value.year()
  const month = currentDate.value.month() + 1
  const res = await getDailyStats(props.ledgerId, year, month)
  dailyData.value = res.data
  maxExpense.value = Math.max(...res.data.map(d => Number(d.expense)), 0)
}

const prevMonth = () => { currentDate.value = currentDate.value.subtract(1, 'month'); loadData() }
const nextMonth = () => { currentDate.value = currentDate.value.add(1, 'month'); loadData() }

const onDayClick = async (cell) => {
  selectedDate.value = cell.date
  drawerVisible.value = true
  drawerLoading.value = true
  try {
    const res = await getTransactionsByDate(props.ledgerId, cell.date)
    dayTransactions.value = res.data
  } catch {
    dayTransactions.value = []
  } finally {
    drawerLoading.value = false
  }
}

const handleAddForDay = () => {
  drawerVisible.value = false
  emit('addTransaction', selectedDate.value)
}

watch(() => props.ledgerId, loadData)

onMounted(loadData)
</script>

<style scoped>
.calendar-root { padding-bottom: 24px; }

.calendar-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.section-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.month-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.month-label {
  font-size: 13px;
  min-width: 90px;
  text-align: center;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 1px 6px rgba(0,0,0,.06);
}

.weekday-header {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  padding: 8px 0;
}

.day-cell {
  min-height: 72px;
  border-radius: 8px;
  padding: 4px 6px;
  cursor: pointer;
  transition: all .15s;
  position: relative;
}

.day-cell:not(.empty):hover {
  box-shadow: 0 2px 8px rgba(0,0,0,.1);
  transform: translateY(-1px);
}

.day-cell.empty {
  cursor: default;
}

.day-cell.today {
  border: 2px solid #409eff;
}

.day-number {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.day-income {
  font-size: 10px;
  color: #43a047;
  line-height: 1.4;
  font-weight: 500;
}

.day-expense {
  font-size: 10px;
  color: #e53935;
  line-height: 1.4;
  font-weight: 500;
}

/* Drawer styles */
.drawer-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 0;
  color: #909399;
}

.drawer-empty {
  padding: 40px 0;
}

.tx-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tx-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 8px;
}

.tx-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tx-icon {
  font-size: 22px;
}

.tx-cat {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.tx-desc {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.tx-amount {
  font-size: 14px;
  font-weight: 600;
}

.tx-amount.green { color: #43a047; }
.tx-amount.red { color: #e53935; }

.tx-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.tx-mood { line-height: 1; }

.mood-img {
  object-fit: contain;
}

@media (max-width: 640px) {
  .calendar-grid {
    gap: 2px;
    padding: 8px;
  }
  .day-cell {
    min-height: 56px;
    padding: 2px 4px;
  }
  .day-number {
    font-size: 12px;
  }
  .day-income, .day-expense {
    font-size: 9px;
  }
  .weekday-header {
    font-size: 11px;
    padding: 4px 0;
  }
}
</style>
