<template>
  <div class="app-root">
    <!-- Header -->
    <div class="app-header">
      <h1 class="app-title">家庭记账本</h1>

      <!-- 账本选择器 -->
      <div class="ledger-area">
        <el-select
          v-model="currentLedgerId"
          size="small"
          style="width:130px"
          @change="onLedgerChange"
        >
          <el-option
            v-for="l in ledgers"
            :key="l.id"
            :label="`${l.icon || '📒'} ${l.name}`"
            :value="l.id"
          />
        </el-select>
        <el-button text size="small" :icon="Setting" @click="showLedgerManager = true" />
      </div>

      <!-- Tab 导航 -->
      <div class="nav-tabs">
        <span
          v-for="tab in TABS"
          :key="tab.key"
          class="nav-tab"
          :class="{ active: activeView === tab.key }"
          @click="activeView = tab.key"
        >{{ tab.label }}</span>
      </div>

      <!-- 月份导航（账单 tab 显示） -->
      <div v-if="activeView === 'transactions'" class="month-nav">
        <el-button :icon="ArrowLeft" circle size="small" @click="prevMonth" />
        <span class="month-label">{{ yearMonth }}</span>
        <el-button :icon="ArrowRight" circle size="small" @click="nextMonth" />
      </div>

      <!-- 操作按钮 -->
      <div class="header-actions">
        <template v-if="activeView === 'transactions'">
          <el-button size="small" :icon="Download" @click="handleExport">导出</el-button>
          <el-button type="primary" size="small" :icon="Plus" @click="openForm(null)">记一笔</el-button>
        </template>
      </div>
    </div>

    <!-- Content -->
    <div class="app-content">
      <!-- 账单 Tab -->
      <template v-if="activeView === 'transactions'">
        <SummaryCards :summary="summary" />

        <!-- 筛选面板 -->
        <div class="filter-bar">
          <el-button
            text
            size="small"
            :icon="showFilters ? ArrowUp : Filter"
            @click="showFilters = !showFilters"
          >{{ showFilters ? '收起筛选' : '筛选' }}</el-button>
        </div>
        <el-collapse-transition>
          <div v-show="showFilters" class="filter-panel">
            <el-input
              v-model="filters.keyword"
              placeholder="备注关键字"
              clearable
              size="small"
              style="width:160px"
              @change="loadData"
            />
            <el-select
              v-model="filters.categoryId"
              placeholder="分类"
              clearable
              size="small"
              style="width:120px"
              @change="loadData"
            >
              <el-option
                v-for="c in categories"
                :key="c.id"
                :label="`${c.icon} ${c.name}`"
                :value="c.id"
              />
            </el-select>
            <el-input-number
              v-model="filters.minAmount"
              placeholder="最小金额"
              :min="0"
              :controls="false"
              size="small"
              style="width:110px"
              @change="loadData"
            />
            <span style="color:#999">—</span>
            <el-input-number
              v-model="filters.maxAmount"
              placeholder="最大金额"
              :min="0"
              :controls="false"
              size="small"
              style="width:110px"
              @change="loadData"
            />
            <el-button size="small" @click="resetFilters">重置</el-button>
          </div>
        </el-collapse-transition>

        <TransactionList
          :transactions="transactions"
          @edit="openForm"
          @delete="handleDelete"
        />
      </template>

      <!-- 图表 Tab -->
      <ChartsView
        v-if="activeView === 'charts'"
        :ledger-id="currentLedgerId"
        :year="year"
        :month="month"
      />

      <!-- 分类 Tab -->
      <CategoryManager
        v-if="activeView === 'categories'"
        :categories="categories"
        @changed="loadCategories"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <TransactionForm
      v-model="showForm"
      :transaction="editingTx"
      :categories="categories"
      :ledger-id="currentLedgerId"
      @saved="loadData"
    />

    <!-- 账本管理弹窗 -->
    <LedgerManager
      v-model="showLedgerManager"
      :ledgers="ledgers"
      @changed="reloadLedgers"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, ArrowRight, Plus, Download, Setting, Filter, ArrowUp } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { useLedger } from './composables/useLedger.js'
import SummaryCards from './components/SummaryCards.vue'
import TransactionList from './components/TransactionList.vue'
import TransactionForm from './components/TransactionForm.vue'
import ChartsView from './components/ChartsView.vue'
import LedgerManager from './components/LedgerManager.vue'
import CategoryManager from './components/CategoryManager.vue'
import { getCategories, getTransactions, getSummary, deleteTransaction, exportTransactions } from './api/index.js'

const TABS = [
  { key: 'transactions', label: '账单' },
  { key: 'charts', label: '图表' },
  { key: 'categories', label: '分类' }
]

const { ledgers, currentLedgerId, load: loadLedgers, select: selectLedger } = useLedger()

const currentDate = ref(dayjs())
const transactions = ref([])
const summary = ref({ totalIncome: 0, totalExpense: 0, balance: 0 })
const categories = ref([])
const activeView = ref('transactions')
const showForm = ref(false)
const editingTx = ref(null)
const showLedgerManager = ref(false)
const showFilters = ref(false)
const filters = ref({ keyword: '', categoryId: null, minAmount: null, maxAmount: null })

const yearMonth = computed(() => currentDate.value.format('YYYY年MM月'))
const year = computed(() => currentDate.value.year())
const month = computed(() => currentDate.value.month() + 1)

const loadData = async () => {
  if (!currentLedgerId.value) return
  const activeFilters = {}
  if (filters.value.keyword) activeFilters.keyword = filters.value.keyword
  if (filters.value.categoryId) activeFilters.categoryId = filters.value.categoryId
  if (filters.value.minAmount != null) activeFilters.minAmount = filters.value.minAmount
  if (filters.value.maxAmount != null) activeFilters.maxAmount = filters.value.maxAmount

  const [txRes, sumRes] = await Promise.all([
    getTransactions(currentLedgerId.value, year.value, month.value, activeFilters),
    getSummary(currentLedgerId.value, year.value, month.value)
  ])
  transactions.value = txRes.data
  summary.value = sumRes.data
}

const loadCategories = async () => {
  const res = await getCategories()
  categories.value = res.data
}

const reloadLedgers = async () => {
  await loadLedgers()
  await loadData()
}

const onLedgerChange = (id) => {
  selectLedger(id)
  loadData()
}

const prevMonth = () => { currentDate.value = currentDate.value.subtract(1, 'month'); loadData() }
const nextMonth = () => { currentDate.value = currentDate.value.add(1, 'month'); loadData() }

const openForm = (tx) => { editingTx.value = tx; showForm.value = true }

const handleDelete = async (tx) => {
  try {
    await ElMessageBox.confirm('确认删除这条记录？', '提示', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteTransaction(tx.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* cancelled */ }
}

const handleExport = async () => {
  try {
    const res = await exportTransactions(currentLedgerId.value, year.value, month.value)
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = `transactions-${year.value}-${String(month.value).padStart(2, '0')}.csv`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
}

const resetFilters = () => {
  filters.value = { keyword: '', categoryId: null, minAmount: null, maxAmount: null }
  loadData()
}

onMounted(async () => {
  await loadLedgers()
  await loadCategories()
  await loadData()
})
</script>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
  color: #333;
}
</style>

<style scoped>
.app-root { min-height: 100vh; }

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 1px 8px rgba(0,0,0,.08);
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  height: 56px;
  flex-wrap: nowrap;
}

.app-title {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
}

.ledger-area {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.nav-tabs {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.nav-tab {
  padding: 4px 14px;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  color: #666;
  transition: all .2s;
}
.nav-tab:hover { background: #f0f2f5; }
.nav-tab.active { background: #409eff1a; color: #409eff; font-weight: 500; }

.month-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.month-label {
  font-size: 14px;
  font-weight: 500;
  min-width: 100px;
  text-align: center;
}

.header-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
  flex-shrink: 0;
}

.app-content {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px 16px;
}

.filter-bar {
  margin-bottom: 4px;
}

.filter-panel {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  background: #fff;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
</style>
