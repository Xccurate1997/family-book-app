---
alwaysApply: true
---

# 家庭记账本前端 - 项目结构

家庭记账本（book-app-frontend）是一个基于 Vue 3 + Element Plus 的家庭财务管理单页应用，提供多账本记账、分类管理、数据统计图表、日历视图和主题定制等功能。

## 技术栈

- **框架**: Vue 3（Composition API + `<script setup>`）
- **UI 组件库**: Element Plus 2.7+（中文语言包 `zh-cn`）
- **图标**: @element-plus/icons-vue（全局注册）
- **图表**: ECharts 5.6+
- **HTTP 请求**: Axios 1.6+
- **日期处理**: Day.js 1.11+
- **构建工具**: Vite 5.2+
- **开发语言**: JavaScript（ES Module）

## 主要目录

- `src/`: 源代码根目录
  - `api/`: API 请求封装，统一使用 Axios 实例
  - `components/`: 所有 Vue 单文件组件
  - `composables/`: Vue 3 组合式函数（Composables）

## 关键文件

- [package.json](mdc:package.json): 项目依赖与脚本配置
- [vite.config.js](mdc:vite.config.js): Vite 构建配置，包含开发代理（`/api` → `localhost:8080`）
- [index.html](mdc:index.html): 入口 HTML
- [src/main.js](mdc:src/main.js): 应用入口，注册 Element Plus 及全局图标
- [src/App.vue](mdc:src/App.vue): 根组件，包含全局布局、Tab 导航、月份切换、筛选面板
- [src/api/index.js](mdc:src/api/index.js): 所有后端 API 调用（账本、分类、交易、统计）

### 组件文件

- [src/components/SummaryCards.vue](mdc:src/components/SummaryCards.vue): 月度收支汇总卡片
- [src/components/TransactionList.vue](mdc:src/components/TransactionList.vue): 交易记录列表（按日期分组）
- [src/components/TransactionForm.vue](mdc:src/components/TransactionForm.vue): 新增/编辑交易弹窗表单
- [src/components/ChartsView.vue](mdc:src/components/ChartsView.vue): 月度统计图表（折线图、饼图、柱状图）
- [src/components/YearlyReport.vue](mdc:src/components/YearlyReport.vue): 年度报表（年度汇总 + 图表）
- [src/components/CalendarView.vue](mdc:src/components/CalendarView.vue): 日历视图（热力图 + 日明细抽屉）
- [src/components/CategoryManager.vue](mdc:src/components/CategoryManager.vue): 收支分类管理
- [src/components/LedgerManager.vue](mdc:src/components/LedgerManager.vue): 账本管理弹窗
- [src/components/ThemeSettings.vue](mdc:src/components/ThemeSettings.vue): 主题设置弹窗（纯色/渐变/图片背景）

### 组合式函数

- [src/composables/useLedger.js](mdc:src/composables/useLedger.js): 账本选择与持久化
- [src/composables/useTheme.js](mdc:src/composables/useTheme.js): 主题配置与 CSS 变量应用
- [src/composables/useEmojiSize.js](mdc:src/composables/useEmojiSize.js): 表情大小切换（S/M/L）

## 构建和运行

```bash
# 安装依赖
npm install

# 开发模式（端口 5173，API 代理到 localhost:8080）
npm run dev

# 生产构建
npm run build

# 预览构建产物
npm run preview
```

## 后端依赖

前端通过 `/api` 前缀代理到后端服务（默认 `http://localhost:8080`），后端提供 RESTful API，涵盖账本、分类、交易记录、统计数据等接口。
