<template>
  <div class="charts-root">
    <!-- 月份选择（图表 tab 独立控制） -->
    <div class="charts-header">
      <span class="section-label">月度图表</span>
      <div class="month-nav">
        <el-button :icon="ArrowLeft" circle size="small" @click="prevMonth" />
        <span class="month-label">{{ displayMonth }}</span>
        <el-button :icon="ArrowRight" circle size="small" @click="nextMonth" />
      </div>
    </div>

    <div class="charts-grid">
      <!-- 收支趋势折线图 -->
      <div class="chart-card">
        <div class="chart-title">收支趋势（近6个月）</div>
        <div v-if="trendEmpty" class="chart-empty"><el-empty description="暂无数据" :image-size="60" /></div>
        <div v-else ref="trendRef" class="chart-canvas" />
      </div>

      <!-- 支出分类饼图 -->
      <div class="chart-card">
        <div class="chart-title">支出分类（本月）</div>
        <div v-if="pieEmpty" class="chart-empty"><el-empty description="本月暂无支出" :image-size="60" /></div>
        <div v-else ref="pieRef" class="chart-canvas" />
      </div>

      <!-- 日支出柱状图 -->
      <div class="chart-card">
        <div class="chart-title">每日支出（本月）</div>
        <div v-if="dailyEmpty" class="chart-empty"><el-empty description="本月暂无数据" :image-size="60" /></div>
        <div v-else ref="dailyRef" class="chart-canvas" />
      </div>

      <!-- 收支对比柱状图 -->
      <div class="chart-card">
        <div class="chart-title">收支对比（近6个月）</div>
        <div v-if="trendEmpty" class="chart-empty"><el-empty description="暂无数据" :image-size="60" /></div>
        <div v-else ref="compareRef" class="chart-canvas" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getMonthlyTrend, getCategoryBreakdown, getDailyStats } from '../api/index.js'

const props = defineProps({ ledgerId: Number, year: Number, month: Number })

const currentDate = ref(dayjs())
const displayMonth = computed(() => currentDate.value.format('YYYY年MM月'))
const curYear = computed(() => currentDate.value.year())
const curMonth = computed(() => currentDate.value.month() + 1)

const trendRef = ref()
const pieRef = ref()
const dailyRef = ref()
const compareRef = ref()

let trendChart = null
let pieChart = null
let dailyChart = null
let compareChart = null

const trendData = ref([])
const pieData = ref([])
const dailyData = ref([])

const trendEmpty = computed(() => trendData.value.every(d => d.income == 0 && d.expense == 0))
const pieEmpty = computed(() => pieData.value.length === 0)
const dailyEmpty = computed(() => dailyData.value.every(d => d.expense == 0))

const loadData = async () => {
  if (!props.ledgerId) return
  const [trendRes, pieRes, dailyRes] = await Promise.all([
    getMonthlyTrend(props.ledgerId, 6),
    getCategoryBreakdown(props.ledgerId, curYear.value, curMonth.value),
    getDailyStats(props.ledgerId, curYear.value, curMonth.value)
  ])
  trendData.value = trendRes.data
  pieData.value = pieRes.data
  dailyData.value = dailyRes.data
  await nextTick()
  renderCharts()
}

const initCharts = () => {
  if (trendRef.value && !trendChart) trendChart = echarts.init(trendRef.value)
  if (pieRef.value && !pieChart) pieChart = echarts.init(pieRef.value)
  if (dailyRef.value && !dailyChart) dailyChart = echarts.init(dailyRef.value)
  if (compareRef.value && !compareChart) compareChart = echarts.init(compareRef.value)
}

const renderCharts = () => {
  initCharts()

  // 折线图：收支趋势
  if (trendChart && !trendEmpty.value) {
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['收入', '支出'], bottom: 0 },
      grid: { top: 20, bottom: 40, left: 50, right: 20 },
      xAxis: { type: 'category', data: trendData.value.map(d => d.month), axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
      series: [
        { name: '收入', type: 'line', smooth: true, data: trendData.value.map(d => d.income),
          itemStyle: { color: '#43a047' }, areaStyle: { opacity: 0.08 } },
        { name: '支出', type: 'line', smooth: true, data: trendData.value.map(d => d.expense),
          itemStyle: { color: '#e53935' }, areaStyle: { opacity: 0.08 } }
      ]
    })
  }

  // 饼图：支出分类
  if (pieChart && !pieEmpty.value) {
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
      legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { fontSize: 12 } },
      series: [{
        type: 'pie', radius: ['35%', '65%'],
        center: ['38%', '50%'],
        data: pieData.value.map(d => ({
          name: `${d.icon || ''} ${d.categoryName}`,
          value: Number(d.amount)
        })),
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold' } }
      }]
    })
  }

  // 柱状图：每日支出
  if (dailyChart && !dailyEmpty.value) {
    dailyChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { top: 20, bottom: 30, left: 50, right: 20 },
      xAxis: { type: 'category', data: dailyData.value.map(d => `${d.day}日`), axisLabel: { fontSize: 10 } },
      yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
      series: [{
        name: '支出', type: 'bar', barMaxWidth: 20,
        data: dailyData.value.map(d => Number(d.expense)),
        itemStyle: { color: '#ef5350', borderRadius: [3, 3, 0, 0] }
      }]
    })
  }

  // 柱状图：收支对比
  if (compareChart && !trendEmpty.value) {
    compareChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['收入', '支出'], bottom: 0 },
      grid: { top: 20, bottom: 40, left: 50, right: 20 },
      xAxis: { type: 'category', data: trendData.value.map(d => d.month), axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
      series: [
        { name: '收入', type: 'bar', barGap: '10%', barMaxWidth: 24,
          data: trendData.value.map(d => d.income),
          itemStyle: { color: '#43a047', borderRadius: [3, 3, 0, 0] } },
        { name: '支出', type: 'bar', barMaxWidth: 24,
          data: trendData.value.map(d => d.expense),
          itemStyle: { color: '#ef5350', borderRadius: [3, 3, 0, 0] } }
      ]
    })
  }
}

const onResize = () => {
  [trendChart, pieChart, dailyChart, compareChart].forEach(c => c?.resize())
}

const prevMonth = () => { currentDate.value = currentDate.value.subtract(1, 'month'); loadData() }
const nextMonth = () => { currentDate.value = currentDate.value.add(1, 'month'); loadData() }

watch(() => props.ledgerId, loadData)

onMounted(() => {
  window.addEventListener('resize', onResize)
  loadData()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  ;[trendChart, pieChart, dailyChart, compareChart].forEach(c => c?.dispose())
})
</script>

<style scoped>
.charts-root { padding-bottom: 24px; }

.charts-header {
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

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 640px) {
  .charts-grid { grid-template-columns: 1fr; }
}

.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 6px rgba(0,0,0,.06);
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

.chart-empty {
  height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
