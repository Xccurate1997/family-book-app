<template>
  <div class="app-root">
    <!-- Header -->
    <div class="app-header">
      <h1 class="app-title">家庭记账本</h1>
      <div class="month-nav">
        <el-button :icon="ArrowLeft" circle size="small" @click="prevMonth" />
        <span class="month-label">{{ yearMonth }}</span>
        <el-button :icon="ArrowRight" circle size="small" @click="nextMonth" />
      </div>
      <el-button type="primary" :icon="Plus" @click="openForm(null)">记一笔</el-button>
    </div>

    <!-- Content -->
    <div class="app-content">
      <SummaryCards :summary="summary" />
      <TransactionList
        :transactions="transactions"
        @edit="openForm"
        @delete="handleDelete"
      />
    </div>

    <!-- Form dialog -->
    <TransactionForm
      v-model="showForm"
      :transaction="editingTx"
      :categories="categories"
      @saved="loadData"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, ArrowRight, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import SummaryCards from './components/SummaryCards.vue'
import TransactionList from './components/TransactionList.vue'
import TransactionForm from './components/TransactionForm.vue'
import { getCategories, getTransactions, getSummary, deleteTransaction } from './api/index.js'

const currentDate = ref(dayjs())
const transactions = ref([])
const summary = ref({ totalIncome: 0, totalExpense: 0, balance: 0 })
const categories = ref([])
const showForm = ref(false)
const editingTx = ref(null)

const yearMonth = computed(() => currentDate.value.format('YYYY年MM月'))
const year = computed(() => currentDate.value.year())
const month = computed(() => currentDate.value.month() + 1)

const loadData = async () => {
  const [txRes, sumRes] = await Promise.all([
    getTransactions(year.value, month.value),
    getSummary(year.value, month.value)
  ])
  transactions.value = txRes.data
  summary.value = sumRes.data
}

const prevMonth = () => {
  currentDate.value = currentDate.value.subtract(1, 'month')
  loadData()
}

const nextMonth = () => {
  currentDate.value = currentDate.value.add(1, 'month')
  loadData()
}

const openForm = (tx) => {
  editingTx.value = tx
  showForm.value = true
}

const handleDelete = async (tx) => {
  try {
    await ElMessageBox.confirm('确认删除这条记录？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTransaction(tx.id)
    ElMessage.success('已删除')
    loadData()
  } catch {
    // user cancelled
  }
}

onMounted(async () => {
  const res = await getCategories()
  categories.value = res.data
  await loadData()
})
</script>

<style>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
  color: #333;
}
</style>

<style scoped>
.app-root {
  min-height: 100vh;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: 60px;
}

.app-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}

.month-nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.month-label {
  font-size: 16px;
  font-weight: 500;
  min-width: 110px;
  text-align: center;
}

.app-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 16px;
}
</style>
