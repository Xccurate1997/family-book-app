---
alwaysApply: true
---

# 家庭记账本前端 - 主要功能模块

家庭记账本是一个面向家庭用户的财务管理应用，前端基于 Vue 3 + Element Plus 构建，提供直观的记账、统计和可视化功能。

## 核心功能

### 1. 多账本管理

支持创建和管理多个独立账本（如"家庭日常"、"旅行基金"等），每个账本有独立的图标、名称和颜色。

相关代码：
- `src/components/LedgerManager.vue`: 账本的增删改管理弹窗
- `src/composables/useLedger.js`: 账本选择状态管理与 localStorage 持久化
- `src/api/index.js`: 账本 CRUD API（`/api/ledgers`）

主要特点：
- 账本切换后自动刷新所有数据
- 当前选中账本 ID 持久化到 localStorage
- 支持自定义账本图标和颜色

### 2. 交易记录管理

核心记账功能，支持收入和支出的记录、编辑、删除和导出。

相关代码：
- `src/components/TransactionForm.vue`: 新增/编辑交易的弹窗表单
- `src/components/TransactionList.vue`: 交易列表（按日期分组展示）
- `src/components/SummaryCards.vue`: 月度收支汇总卡片（收入/支出/结余）
- `src/App.vue`: 月份导航、筛选面板、数据加载与导出逻辑

主要特点：
- 按月份浏览，支持前后月切换
- 交易按日期分组，每日显示净收支
- 支持多维度筛选：关键字、分类、金额范围
- 支持 CSV 格式导出
- 支持心情表情（文本 Emoji 和图片表情）
- 表情大小可切换（S/M/L 三档）

### 3. 分类管理

管理收入和支出的分类，每个分类有图标和名称。

相关代码：
- `src/components/CategoryManager.vue`: 分类的增删改管理界面
- `src/api/index.js`: 分类 CRUD API（`/api/categories`）

主要特点：
- 收入和支出分类独立管理
- 每个分类支持自定义 Emoji 图标
- 分类数据在新增/编辑交易时用于下拉选择

### 4. 月度统计图表

提供多维度的月度数据可视化分析。

相关代码：
- `src/components/ChartsView.vue`: 图表视图（4 个图表）
- `src/api/index.js`: 统计 API（`/api/stats/monthly-trend`、`/api/stats/category-breakdown`、`/api/stats/daily`）

主要特点：
- **收支趋势折线图**: 近 6 个月收入/支出趋势
- **支出分类饼图**: 当月支出按分类占比
- **每日支出柱状图**: 当月每日支出金额
- **收支对比柱状图**: 近 6 个月收入与支出对比
- 图表独立月份导航，支持自适应窗口大小

### 5. 年度报表

提供年度维度的财务汇总和趋势分析。

相关代码：
- `src/components/YearlyReport.vue`: 年度报表视图
- `src/api/index.js`: 年度统计 API（`/api/stats/yearly-summary`、`/api/stats/yearly-monthly-trend`、`/api/stats/yearly-category-ranking`）

主要特点：
- **年度汇总卡片**: 年度总收入、总支出、年度结余
- **月度收支对比柱状图**: 12 个月收入与支出对比
- **月度结余趋势折线图**: 12 个月结余变化（正负颜色区分）
- **支出分类排行**: Top 10 支出分类水平柱状图

### 6. 记账日历

以日历形式展示每日收支概况，支持热力图效果。

相关代码：
- `src/components/CalendarView.vue`: 日历视图
- `src/api/index.js`: 日统计 API（`/api/stats/daily`）、按日期查询交易 API（`/api/transactions/by-date`）

主要特点：
- 日历网格展示每日收入/支出金额
- 支出热力图（颜色深浅反映支出大小）
- 今日高亮标记
- 点击日期打开侧边抽屉查看当日明细
- 支持从日历快速新增交易记录

### 7. 主题定制

支持自定义应用背景主题，提供三种背景模式。

相关代码：
- `src/components/ThemeSettings.vue`: 主题设置弹窗
- `src/composables/useTheme.js`: 主题状态管理与 CSS 变量应用

主要特点：
- **纯色模式**: 自定义背景颜色
- **渐变模式**: 双色渐变，支持 5 种渐变方向（含径向渐变）
- **图片模式**: 上传自定义背景图片
- 内容遮罩透明度可调节
- 实时预览效果
- 主题配置持久化到 localStorage

## 页面导航

应用采用顶部 Tab 导航，包含 5 个视图：

| Tab | 组件 | 功能 |
|-----|------|------|
| 账单 | `TransactionList` + `SummaryCards` | 月度交易列表与汇总 |
| 图表 | `ChartsView` | 月度统计图表 |
| 年报 | `YearlyReport` | 年度报表 |
| 日历 | `CalendarView` | 日历视图 |
| 分类 | `CategoryManager` | 分类管理 |

## API 接口概览

所有 API 通过 `/api` 前缀代理到后端，主要接口分组：

- **账本**: `/api/ledgers` — CRUD
- **分类**: `/api/categories` — CRUD
- **交易**: `/api/transactions` — CRUD + 按月查询 + 按日期查询 + 汇总 + 导出
- **统计**: `/api/stats/` — 月度趋势、分类占比、日统计、年度汇总、年度月趋势、年度分类排行
