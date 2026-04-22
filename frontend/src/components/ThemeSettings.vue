<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="切换主题"
    width="480px"
  >
    <div v-if="availableThemes.length === 0" class="empty-tip">
      暂无可用主题
    </div>

    <div v-else class="theme-list">
      <div
        v-for="theme in availableThemes"
        :key="theme.id"
        class="theme-card"
        :class="{ active: currentThemeId === theme.id }"
        @click="handleSelect(theme)"
      >
        <div class="theme-color-bar" :style="{ background: theme.primaryColor || '#409eff' }"></div>
        <div class="theme-info">
          <div class="theme-name">
            {{ theme.name }}
            <el-tag v-if="theme.isDefault" size="small" type="info">默认</el-tag>
          </div>
          <div v-if="theme.description" class="theme-desc">{{ theme.description }}</div>
        </div>
        <el-icon v-if="currentThemeId === theme.id" class="check-icon" color="#67c23a"><Check /></el-icon>
      </div>
    </div>

    <template #footer>
      <el-button size="small" @click="$emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { watch } from 'vue'
import { Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useTheme } from '../composables/useTheme.js'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const { availableThemes, currentThemeId, loadAvailableThemes, selectTheme } = useTheme()

watch(() => props.modelValue, (visible) => {
  if (visible) {
    loadAvailableThemes()
  }
})

async function handleSelect(theme) {
  await selectTheme(theme.id)
  ElMessage.success(`已切换到「${theme.name}」`)
  emit('update:modelValue', false)
}
</script>

<style scoped>
.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
  font-size: 14px;
}
.theme-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.theme-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 2px solid #eee;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.theme-card:hover {
  border-color: #c0c4cc;
  background: #fafafa;
}
.theme-card.active {
  border-color: #67c23a;
  background: #f0f9eb;
}
.theme-color-bar {
  width: 8px;
  height: 40px;
  border-radius: 4px;
  flex-shrink: 0;
}
.theme-info {
  flex: 1;
  min-width: 0;
}
.theme-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  display: flex;
  align-items: center;
  gap: 6px;
}
.theme-desc {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.check-icon {
  font-size: 20px;
  flex-shrink: 0;
}
</style>
