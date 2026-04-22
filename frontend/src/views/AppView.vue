<template>
  <div class="app-root">
    <!-- 开屏动画 -->
    <Transition name="splash">
      <div v-if="showSplash" class="splash-overlay" @click="dismissSplash">
        <template v-if="currentTheme?.splashType === 'CUSTOM_IMAGE' && currentTheme?.splashImageFilename">
          <img :src="`/api/theme-assets/${currentTheme.splashImageFilename}`" class="splash-image" />
        </template>
        <template v-else-if="currentTheme?.splashType === 'FADE_IN'">
          <div class="splash-text fade-in-anim">家庭记账本</div>
        </template>
        <template v-else-if="currentTheme?.splashType === 'SLIDE_UP'">
          <div class="splash-text slide-up-anim">家庭记账本</div>
        </template>
      </div>
    </Transition>

    <!-- 左侧装饰图片 -->
    <div v-if="currentDecoration?.leftImageFilename" class="deco-left" :style="decoContainerStyle">
      <img :src="`/api/theme-assets/${currentDecoration.leftImageFilename}`" class="deco-img" :style="decoImgStyle('left')" />
    </div>

    <!-- 右侧装饰图片 -->
    <div v-if="currentDecoration?.rightImageFilename" class="deco-right" :style="decoContainerStyle">
      <img :src="`/api/theme-assets/${currentDecoration.rightImageFilename}`" class="deco-img" :style="decoImgStyle('right')" />
    </div>

    <!-- 左侧装饰图片（背景融合层） -->
    <div v-if="currentDecoration?.leftImageFilename" class="deco-bg-left">
      <img :src="`/api/theme-assets/${currentDecoration.leftImageFilename}`" />
    </div>

    <!-- 右侧装饰图片（背景融合层） -->
    <div v-if="currentDecoration?.rightImageFilename" class="deco-bg-right">
      <img :src="`/api/theme-assets/${currentDecoration.rightImageFilename}`" />
    </div>

    <!-- 背景遮罩层 -->
    <div class="bg-overlay"></div>

    <!-- Header -->
    <div class="app-header">
      <!-- Header 背景纹理 -->
      <div v-if="currentTheme?.headerPatternFilename" class="header-pattern"
           :style="{ backgroundImage: `url(/api/theme-assets/${currentTheme.headerPatternFilename})`, opacity: currentTheme.headerPatternOpacity ?? 0.06 }"></div>

      <!-- Header Logo/吉祥物 -->
      <img v-if="currentTheme?.logoFilename"
           :src="`/api/theme-assets/${currentTheme.logoFilename}`"
           class="header-logo" />

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
        <el-button text size="small" :icon="Brush" @click="showThemeSettings = true" title="主题设置" />
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
        <!-- 表情大小切换 -->
        <el-button-group size="small" class="emoji-size-group">
          <el-tooltip content="小表情" placement="bottom">
            <el-button :type="emojiSize === 'small' ? 'primary' : ''" @click="setEmojiSize('small')">S</el-button>
          </el-tooltip>
          <el-tooltip content="中表情" placement="bottom">
            <el-button :type="emojiSize === 'medium' ? 'primary' : ''" @click="setEmojiSize('medium')">M</el-button>
          </el-tooltip>
          <el-tooltip content="大表情" placement="bottom">
            <el-button :type="emojiSize === 'large' ? 'primary' : ''" @click="setEmojiSize('large')">L</el-button>
          </el-tooltip>
        </el-button-group>

        <template v-if="activeView === 'transactions'">
          <el-button size="small" :icon="Download" @click="handleExport">导出</el-button>
          <el-button type="primary" size="small" :icon="Plus" @click="openForm(null)">记一笔</el-button>
        </template>

        <!-- 用户信息 & 退出 -->
        <el-dropdown trigger="click" @command="handleUserCommand">
          <el-button text size="small" class="user-btn">
            {{ currentUser?.nickname || '用户' }} ▾
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                {{ currentUser?.username }}
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
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
          :empty-state-image="currentTheme?.emptyStateFilename ? `/api/theme-assets/${currentTheme.emptyStateFilename}` : null"
          :income-bg-image="currentTheme?.incomeBgFilename ? `/api/theme-assets/${currentTheme.incomeBgFilename}` : null"
          :expense-bg-image="currentTheme?.expenseBgFilename ? `/api/theme-assets/${currentTheme.expenseBgFilename}` : null"
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

      <!-- 年报 Tab -->
      <YearlyReport
        v-if="activeView === 'yearly'"
        :ledger-id="currentLedgerId"
      />

      <!-- 日历 Tab -->
      <CalendarView
        v-if="activeView === 'calendar'"
        :ledger-id="currentLedgerId"
        :categories="categories"
        @add-transaction="openFormWithDate"
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
      :default-date="formDefaultDate"
      @saved="onTransactionSaved"
    />

    <!-- 账本管理弹窗 -->
    <LedgerManager
      v-model="showLedgerManager"
      :ledgers="ledgers"
      @changed="reloadLedgers"
    />

    <!-- 主题设置弹窗 -->
    <ThemeSettings v-model="showThemeSettings" />

    <!-- 视频特效彩蛋 -->
    <VideoEffect />

    <!-- 底部角落装饰 -->
    <template v-if="currentTheme?.cornerDecoFilename">
      <div class="corner-deco corner-deco-left">
        <img :src="`/api/theme-assets/${currentTheme.cornerDecoFilename}`" />
      </div>
      <div class="corner-deco corner-deco-right">
        <img :src="`/api/theme-assets/${currentTheme.cornerDecoFilename}`" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Plus, Download, Setting, Filter, ArrowUp, Brush } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { useLedger } from '../composables/useLedger.js'
import { useTheme } from '../composables/useTheme.js'
import { useEmojiSize } from '../composables/useEmojiSize.js'
import { useAuth } from '../composables/useAuth.js'
import SummaryCards from '../components/SummaryCards.vue'
import TransactionList from '../components/TransactionList.vue'
import TransactionForm from '../components/TransactionForm.vue'
import ChartsView from '../components/ChartsView.vue'
import LedgerManager from '../components/LedgerManager.vue'
import CategoryManager from '../components/CategoryManager.vue'
import ThemeSettings from '../components/ThemeSettings.vue'
import YearlyReport from '../components/YearlyReport.vue'
import CalendarView from '../components/CalendarView.vue'
import VideoEffect from '../components/VideoEffect.vue'
import { getCategories, getTransactions, getSummary, deleteTransaction, exportTransactions } from '../api/index.js'
import { useVideoEffect } from '../composables/useVideoEffect.js'

const router = useRouter()
const { currentUser, clearAuth } = useAuth()

const TABS = [
  { key: 'transactions', label: '账单' },
  { key: 'charts', label: '图表' },
  { key: 'yearly', label: '年报' },
  { key: 'calendar', label: '日历' },
  { key: 'categories', label: '分类' }
]

const { ledgers, currentLedgerId, load: loadLedgers, select: selectLedger } = useLedger()
const { loadAvailableThemes, currentTheme, getDecorationForTab, shouldShowSplash, markSplashShown, playSound } = useTheme()
const { currentSize: emojiSize, setSize: setEmojiSize } = useEmojiSize()
const { connectSSE } = useVideoEffect()

const showSplash = ref(false)

const currentDate = ref(dayjs())
const transactions = ref([])
const summary = ref({ totalIncome: 0, totalExpense: 0, balance: 0 })
const categories = ref([])
const activeView = ref('transactions')
const showForm = ref(false)
const editingTx = ref(null)
const showLedgerManager = ref(false)
const showThemeSettings = ref(false)
const showFilters = ref(false)
const filters = ref({ keyword: '', categoryId: null, minAmount: null, maxAmount: null })

const currentDecoration = computed(() => getDecorationForTab(activeView.value))

// 装饰图片容器动态样式
const decoContainerStyle = computed(() => {
  const maxWidth = currentTheme.value?.decoMaxWidth ?? 280
  return { maxWidth: maxWidth + 'px' }
})

// 装饰图片动态样式（渐变 + 透明度 + 高度）
const decoImgStyle = (side) => {
  const theme = currentTheme.value
  const opacity = theme?.decoOpacity ?? 0.6
  const maxHeight = theme?.decoMaxHeight ?? 60
  const fadeEdge = theme?.decoFadeEdge ?? 15
  const fadeSide = theme?.decoFadeSide ?? 70
  const verticalMask = `linear-gradient(to bottom, transparent 0%, black ${fadeEdge}%, black ${100 - fadeEdge}%, transparent 100%)`
  const horizontalMask = side === 'left'
    ? `linear-gradient(to right, black 0%, black ${fadeSide}%, transparent 100%)`
    : `linear-gradient(to left, black 0%, black ${fadeSide}%, transparent 100%)`
  return {
    opacity,
    maxHeight: maxHeight + 'vh',
    maskImage: `${verticalMask}, ${horizontalMask}`,
    WebkitMaskImage: `${verticalMask}, ${horizontalMask}`,
    maskComposite: 'intersect',
    WebkitMaskComposite: 'source-in'
  }
}

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

const formDefaultDate = ref(null)
const openForm = (tx) => { editingTx.value = tx; formDefaultDate.value = null; showForm.value = true }
const openFormWithDate = (date) => { editingTx.value = null; formDefaultDate.value = date; showForm.value = true }

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

function handleUserCommand(command) {
  if (command === 'logout') {
    clearAuth()
    ElMessage.success('已退出登录')
    router.replace('/login')
  }
}

function onTransactionSaved() {
  loadData()
  playSound()
}

function dismissSplash() {
  showSplash.value = false
}

onMounted(async () => {
  await loadAvailableThemes()

  // 开屏动画
  if (shouldShowSplash()) {
    showSplash.value = true
    markSplashShown()
    const duration = currentTheme.value?.splashDuration || 2000
    setTimeout(() => { showSplash.value = false }, duration)
  }

  await loadLedgers()
  await loadCategories()
  await loadData()
  connectSSE()
})
</script>

<style scoped>
.app-root { min-height: 100vh; position: relative; }

.bg-overlay {
  position: fixed;
  inset: 0;
  background: #fff;
  opacity: var(--app-overlay-opacity, 0);
  pointer-events: none;
  z-index: 0;
}

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
  overflow: hidden;
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
  align-items: center;
}

.user-btn {
  font-size: 13px;
  color: #666;
}

.app-content {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px 16px;
  position: relative;
  z-index: 1;
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

/* ── 开屏动画 ── */
.splash-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: var(--theme-header-bg, #fff);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.splash-image {
  max-width: 80%;
  max-height: 80%;
  object-fit: contain;
}
.splash-text {
  font-size: 36px;
  font-weight: 700;
  color: var(--theme-primary, #409eff);
}
.fade-in-anim {
  animation: fadeInScale 1.2s ease-out both;
}
.slide-up-anim {
  animation: slideUp 1s ease-out both;
}
@keyframes fadeInScale {
  from { opacity: 0; transform: scale(0.8); }
  to   { opacity: 1; transform: scale(1); }
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(60px); }
  to   { opacity: 1; transform: translateY(0); }
}
.splash-leave-active {
  transition: opacity 0.5s ease;
}
.splash-leave-to {
  opacity: 0;
}

/* ── 两侧装饰图片（前景层，清晰可见） ── */
.deco-left, .deco-right {
  position: fixed;
  top: 56px; /* header 高度 */
  bottom: 0;
  z-index: 2;
  pointer-events: none;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 动态宽度：填满内容区两侧空白 */
  width: calc((100vw - 860px) / 2 - 8px);
  max-width: 280px;
  min-width: 80px;
}
.deco-left { left: 0; padding-left: 8px; }
.deco-right { right: 0; padding-right: 8px; }
.deco-img {
  width: 100%;
  max-height: 60vh;
  object-fit: contain;
  opacity: 0.6;
  animation: decoFloat 6s ease-in-out infinite, decoBreathe 4s ease-in-out infinite;
  /* 渐变和透明度通过 JS 动态绑定 :style */
}

/* ── 两侧装饰图片（背景融合层，低透明度水印） ── */
.deco-bg-left, .deco-bg-right {
  position: fixed;
  top: 56px;
  bottom: 0;
  z-index: 0;
  pointer-events: none;
  display: flex;
  align-items: center;
  width: calc((100vw - 860px) / 2);
  overflow: hidden;
}
.deco-bg-left { left: 0; justify-content: flex-end; }
.deco-bg-right { right: 0; justify-content: flex-start; }
.deco-bg-left img, .deco-bg-right img {
  width: 120%;
  max-height: 80vh;
  object-fit: contain;
  opacity: 0.08;
  filter: blur(2px);
}

@keyframes decoFloat {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-10px); }
}
@keyframes decoBreathe {
  0%, 100% { opacity: 0.5; }
  50%      { opacity: 0.7; }
}
/* 窄屏隐藏（内容区占满时两侧无空间） */
@media (max-width: 960px) {
  .deco-left, .deco-right,
  .deco-bg-left, .deco-bg-right { display: none; }
}

/* ── Header 点缀 ── */
.header-pattern {
  position: absolute;
  inset: 0;
  background-size: 200px auto;
  background-repeat: repeat;
  pointer-events: none;
  z-index: 0;
  /* opacity 通过 :style 动态绑定 */
}
.header-logo {
  width: 36px;
  height: 36px;
  object-fit: contain;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  animation: logoWiggle 3s ease-in-out infinite;
}
@keyframes logoWiggle {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(3deg); }
  75% { transform: rotate(-3deg); }
}

/* ── 底部角落装饰 ── */
.corner-deco {
  position: fixed;
  bottom: 12px;
  z-index: 1;
  pointer-events: none;
  opacity: 0.4;
}
.corner-deco img {
  width: 60px;
  height: 60px;
  object-fit: contain;
}
.corner-deco-left {
  left: 16px;
}
.corner-deco-right {
  right: 16px;
  transform: scaleX(-1);
}
@media (max-width: 960px) {
  .corner-deco { display: none; }
}
</style>
