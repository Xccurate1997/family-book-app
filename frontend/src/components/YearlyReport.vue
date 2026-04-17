<template>
  <div class="yearly-root">
    <!-- 年份导航 -->
    <div class="yearly-header">
      <span class="section-label">年度报表</span>
      <div class="year-nav">
        <el-button :icon="ArrowLeft" circle size="small" @click="currentYear--; loadData()" />
        <span class="year-label">{{ currentYear }}年</span>
        <el-button :icon="ArrowRight" circle size="small" @click="currentYear++; loadData()" />
      </div>
    </div>

    <!-- 年度总览卡片 -->
    <div class="summary-row">
      <div class="summary-card income">
        <div class="card-label">年度收入</div>
        <div class="card-amount">{{ formatMoney(summaryData.totalIncome) }}</div>
      </div>
      <div class="summary-card expense">
        <div class="card-label">年度支出</div>
        <div class="card-amount">{{ formatMoney(summaryData.totalExpense) }}</div>
      </div>
      <div class="summary-card" :class="summaryData.balance >= 0 ? 'balance' : 'balance-neg'">
        <div class="card-label">年度结余</div>
        <div class="card-amount">{{ formatMoney(summaryData.balance) }}</div>
      </div>
    </div>

    <!-- 图表 -->
    <div class="charts-grid">
      <!-- 12月收支对比 -->
      <div class="chart-card">
        <div class="chart-title">月度收支对比</div>
        <div v-if="trendEmpty" class="chart-empty"><el-empty description="暂无数据" :image-size="60" /></div>
        <div v-else ref="trendRef" class="chart-canvas" />
      </div>

      <!-- 月度结余趋势 -->
      <div class="chart-card">
        <div class="chart-title">月度结余趋势</div>
        <div v-if="trendEmpty" class="chart-empty"><el-empty description="暂无数据" :image-size="60" /></div>
        <div v-else ref="balanceRef" class="chart-canvas" />
      </div>

      <!-- 分类支出排行 -->
      <div class="chart-card full-width">
        <div class="chart-title">支出分类排行（Top 10）</div>
        <div v-if="rankingEmpty" class="chart-empty"><el-empty description="暂无支出数据" :image-size="60" /></div>
        <div v-else ref="rankingRef" class="chart-canvas ranking-canvas" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getYearlySummary, getYearlyMonthlyTrend, getYearlyCategoryRanking } from '../api/index.js'

const props = defineProps({ ledgerId: Number })

const currentYear = ref(dayjs().year())
const summaryData = ref({ totalIncome: 0, totalExpense: 0, balance: 0 })
const trendData = ref([])
const rankingData = ref([])

const trendEmpty = computed(() => trendData.value.every(d => d.income == 0 && d.expense == 0))
const rankingEmpty = computed(() => rankingData.value.length === 0)

const trendRef = ref()
const balanceRef = ref()
const rankingRef = ref()

let trendChart = null
let balanceChart = null
let rankingChart = null

const formatMoney = (v) => {
  const n = Number(v) || 0
  return (n < 0 ? '-' : '') + '¥' + Math.abs(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const loadData = async () => {
  if (!props.ledgerId) return
  const [sumRes, trendRes, rankRes] = await Promise.all([
    getYearlySummary(props.ledgerId, currentYear.value),
    getYearlyMonthlyTrend(props.ledgerId, currentYear.value),
    getYearlyCategoryRanking(props.ledgerId, currentYear.value, 10)
  ])
  summaryData.value = sumRes.data
  trendData.value = trendRes.data
  rankingData.value = rankRes.data
  await nextTick()
  renderCharts()
}

const initCharts = () => {
  if (trendRef.value && !trendChart) trendChart = echarts.init(trendRef.value)
  if (balanceRef.value && !balanceChart) balanceChart = echarts.init(balanceRef.value)
  if (rankingRef.value && !rankingChart) rankingChart = echarts.init(rankingRef.value)
}

const renderCharts = () => {
  initCharts()

  const months = trendData.value.map(d => {
    const m = d.month.split('-')[1]
    return `${parseInt(m)}月`
  })

  // 收支对比柱状图
  if (trendChart && !trendEmpty.value) {
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['收入', '支出'], bottom: 0 },
      grid: { top: 20, bottom: 40, left: 50, right: 20 },
      xAxis: { type: 'category', data: months, axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
      series: [
        { name: '收入', type: 'bar', barGap: '10%', barMaxWidth: 20,
          data: trendData.value.map(d => d.income),
          itemStyle: { color: '#43a047', borderRadius: [3, 3, 0, 0] } },
        { name: '支出', type: 'bar', barMaxWidth: 20,
          data: trendData.value.map(d => d.expense),
          itemStyle: { color: '#ef5350', borderRadius: [3, 3, 0, 0] } }
      ]
    })
  }

  // 结余趋势折线图
  if (balanceChart && !trendEmpty.value) {
    const balances = trendData.value.map(d => Number(d.income) - Number(d.expense))
    balanceChart.setOption({
      tooltip: { trigger: 'axis', formatter: (p) => `${p[0].name}<br/>结余: ¥${p[0].value.toLocaleString()}` },
      grid: { top: 20, bottom: 30, left: 50, right: 20 },
      xAxis: { type: 'category', data: months, axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
      visualMap: {
        show: false, pieces: [
          { lte: 0, color: '#ef5350' },
          { gt: 0, color: '#43a047' }
        ]
      },
      series: [{
        type: 'line', smooth: true, data: balances,
        areaStyle: { opacity: 0.1 },
        markLine: { data: [{ type: 'average', name: '平均' }], lineStyle: { type: 'dashed' } }
      }]
    })
  }

  // 分类排行水平柱状图
  if (rankingChart && !rankingEmpty.value) {
    const sorted = [...rankingData.value].reverse()
    rankingChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { top: 10, bottom: 20, left: 100, right: 40 },
      xAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
      yAxis: {
        type: 'category',
        data: sorted.map(d => `${d.icon || ''} ${d.categoryName}`),
        axisLabel: { fontSize: 12 }
      },
      series: [{
        type: 'bar', barMaxWidth: 24,
        data: sorted.map(d => Number(d.amount)),
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#ef5350' },
            { offset: 1, color: '#ff8a65' }
          ])
        },
        label: { show: true, position: 'right', formatter: '¥{c}', fontSize: 11 }
      }]
    })
  }
}

const onResize = () => {
  [trendChart, balanceChart, rankingChart].forEach(c => c?.resize())
}

watch(() => props.ledgerId, loadData)

onMounted(() => {
  window.addEventListener('resize', onResize)
  loadData()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  ;[trendChart, balanceChart, rankingChart].forEach(c => c?.dispose())
})
</script>

<style scoped>
.yearly-root { padding-bottom: 24px; }

.yearly-header {
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

.year-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.year-label {
  font-size: 14px;
  font-weight: 500;
  min-width: 70px;
  text-align: center;
}

.summary-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
}

.summary-card {
  border-radius: 12px;
  padding: 16px;
  color: #fff;
}

.summary-card.income { background: linear-gradient(135deg, #43a047, #66bb6a); }
.summary-card.expense { background: linear-gradient(135deg, #e53935, #ef5350); }
.summary-card.balance { background: linear-gradient(135deg, #1e88e5, #42a5f5); }
.summary-card.balance-neg { background: linear-gradient(135deg, #f57c00, #ffa726); }

.card-label {
  font-size: 12px;
  opacity: 0.9;
  margin-bottom: 6px;
}

.card-amount {
  font-size: 20px;
  font-weight: 700;
}

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 640px) {
  .charts-grid { grid-template-columns: 1fr; }
  .summary-row { grid-template-columns: 1fr; }
}

.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 6px rgba(0,0,0,.06);
}

.chart-card.full-width {
  grid-column: 1 / -1;
}

.chart-title {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
}

.chart-canvas {
  height: 260px;
}

.ranking-canvas {
  height: 320px;
}

.chart-empty {
  height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
