---
alwaysApply: true
---

# 家庭记账本前端 - 代码风格规范

本项目遵循以下代码风格规范和约定：

## Vue 组件规范

### 单文件组件结构

所有组件统一采用 `<template>` → `<script setup>` → `<style scoped>` 的顺序：

```vue
<template>
  <!-- 模板内容 -->
</template>

<script setup>
// 组合式 API 逻辑
</script>

<style scoped>
/* 组件样式 */
</style>
```

### Composition API

- **必须使用** `<script setup>` 语法糖，禁止使用 Options API
- 使用 `defineProps()` 和 `defineEmits()` 声明组件接口
- 使用 `v-model` 双向绑定模式（`modelValue` + `update:modelValue`）实现弹窗等组件的显隐控制
- 复用逻辑抽取为 `composables/` 下的组合式函数，以 `use` 前缀命名（如 `useLedger`、`useTheme`）

### 组件通信

- **Props 向下传递**：父组件通过 props 传递数据
- **Events 向上通知**：子组件通过 `emit` 通知父组件（如 `@changed`、`@saved`、`@edit`、`@delete`）
- **共享状态**：通过 composables 中的模块级 `ref` 实现跨组件状态共享（非 Vuex/Pinia）

## JavaScript 规范

### 模块化

- 使用 ES Module（`import` / `export`），项目 `"type": "module"`
- API 函数按资源分组，使用具名导出（`export const getXxx = ...`）

### 命名约定

- **文件名**: 组件使用 PascalCase（如 `TransactionList.vue`），composables 使用 camelCase（如 `useLedger.js`）
- **变量/函数**: camelCase（如 `currentLedgerId`、`loadData`）
- **常量**: UPPER_SNAKE_CASE（如 `TABS`、`WEEKDAYS`、`STORAGE_KEY`）
- **CSS 类名**: kebab-case（如 `app-header`、`tx-item`、`chart-card`）

### 异步处理

- 使用 `async/await` 处理异步操作
- 多个独立请求使用 `Promise.all()` 并发执行
- 错误处理使用 `try/catch`，通过 `ElMessage` 展示用户友好的提示

### 日期处理

- 统一使用 Day.js 进行日期操作和格式化
- 日期格式约定：API 传输使用 `YYYY-MM-DD`，显示使用中文格式（如 `YYYY年MM月`）

## 样式规范

### CSS 作用域

- 组件样式必须使用 `<style scoped>` 防止样式泄漏
- 全局样式仅在 `App.vue` 的非 scoped `<style>` 中定义

### 设计风格

- **圆角**: 卡片使用 `border-radius: 12px`，按钮/标签使用 `border-radius: 8px` 或 `16px`
- **阴影**: 使用轻量阴影 `box-shadow: 0 1px 4px rgba(0,0,0,.06)` 或 `0 1px 6px rgba(0,0,0,.06)`
- **颜色语义**: 收入绿色 `#43a047`，支出红色 `#e53935`，结余蓝色 `#1e88e5`，负结余橙色 `#fb8c00`
- **渐变卡片**: 使用 `linear-gradient(135deg, ...)` 创建渐变背景
- **过渡动画**: 使用 `transition: all .2s` 实现平滑交互效果
- **响应式**: 使用 `@media (max-width: 640px)` 适配移动端

### CSS 变量

- `--app-bg`: 应用背景（由主题系统动态设置）
- `--app-overlay-opacity`: 内容遮罩透明度

## 数据持久化

- 使用 `localStorage` 存储用户偏好设置，key 统一以 `bookapp_` 前缀命名：
  - `bookapp_ledger_id`: 当前选中的账本 ID
  - `bookapp_theme`: 主题配置 JSON
  - `bookapp_emoji_size`: 表情大小偏好

## Element Plus 使用约定

- 全局注册所有 Element Plus 图标组件
- 使用中文语言包 `zh-cn`
- 弹窗组件统一设置 `:close-on-click-modal="false"` 防止误关
- 表单验证使用 `el-form` 的 `rules` 属性
- 操作反馈统一使用 `ElMessage`（成功/警告/错误），危险操作使用 `ElMessageBox.confirm`

## ECharts 使用约定

- 图表实例在 `onMounted` 中初始化，在 `onUnmounted` 中销毁（`dispose`）
- 监听 `window.resize` 事件调用 `chart.resize()` 实现自适应
- 图表容器使用固定高度（`260px` 或 `320px`）
- 空数据时显示 `el-empty` 占位，不渲染空图表
