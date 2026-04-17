<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="主题设置"
    width="480px"
    :close-on-click-modal="false"
  >
    <!-- 模式选择 -->
    <div class="theme-section">
      <div class="theme-label">背景模式</div>
      <el-radio-group v-model="form.mode" size="small">
        <el-radio-button value="solid">纯色</el-radio-button>
        <el-radio-button value="gradient">渐变</el-radio-button>
        <el-radio-button value="image">图片</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 纯色 -->
    <div v-if="form.mode === 'solid'" class="theme-section">
      <div class="theme-label">背景颜色</div>
      <el-color-picker v-model="form.solidColor" show-alpha />
    </div>

    <!-- 渐变 -->
    <template v-if="form.mode === 'gradient'">
      <div class="theme-section">
        <div class="theme-label">渐变颜色</div>
        <div class="gradient-colors">
          <el-color-picker v-model="form.gradientColor1" />
          <span class="arrow">→</span>
          <el-color-picker v-model="form.gradientColor2" />
        </div>
      </div>
      <div class="theme-section">
        <div class="theme-label">渐变方向</div>
        <el-select v-model="form.gradientDirection" size="small" style="width:160px">
          <el-option label="↓ 上到下" value="to bottom" />
          <el-option label="→ 左到右" value="to right" />
          <el-option label="↘ 左上到右下" value="to bottom right" />
          <el-option label="↗ 左下到右上" value="to top right" />
          <el-option label="● 径向渐变" value="circle" />
        </el-select>
      </div>
    </template>

    <!-- 图片 -->
    <div v-if="form.mode === 'image'" class="theme-section">
      <div class="theme-label">背景图片</div>
      <div class="image-upload">
        <el-button size="small" @click="$refs.fileInput.click()">选择图片</el-button>
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          style="display:none"
          @change="handleImageSelect"
        />
        <span v-if="form.imageData" class="image-info">已选择图片</span>
        <span v-else class="image-info hint">未选择</span>
      </div>
      <div v-if="imageSizeWarning" class="size-warning">
        图片较大（{{ imageSizeMB }}MB），可能导致存储失败，建议使用 2MB 以内的图片
      </div>
    </div>

    <!-- 遮罩透明度 -->
    <div v-if="form.mode !== 'solid'" class="theme-section">
      <div class="theme-label">内容遮罩透明度 <span class="hint">（越大内容越清晰）</span></div>
      <el-slider v-model="overlayPercent" :min="0" :max="100" :step="5" show-stops />
    </div>

    <!-- 预览 -->
    <div class="theme-section">
      <div class="theme-label">预览</div>
      <div class="preview-box" :style="previewStyle">
        <div class="preview-overlay" :style="{ opacity: form.overlayOpacity }"></div>
        <div class="preview-text">家庭记账本</div>
      </div>
    </div>

    <template #footer>
      <el-button size="small" @click="handleReset">重置默认</el-button>
      <el-button size="small" @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" size="small" @click="handleApply">应用</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useTheme } from '../composables/useTheme.js'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const { themeSettings, applyTheme, resetTheme } = useTheme()

const form = reactive({
  mode: 'solid',
  solidColor: '#f0f2f5',
  gradientColor1: '#667eea',
  gradientColor2: '#764ba2',
  gradientDirection: 'to bottom right',
  imageData: null,
  overlayOpacity: 0.7
})

const imageSizeMB = ref(0)
const imageSizeWarning = computed(() => imageSizeMB.value > 2)

const overlayPercent = computed({
  get: () => Math.round(form.overlayOpacity * 100),
  set: (v) => { form.overlayOpacity = v / 100 }
})

const previewStyle = computed(() => {
  let bg
  switch (form.mode) {
    case 'gradient':
      bg = form.gradientDirection === 'circle'
        ? `radial-gradient(circle, ${form.gradientColor1}, ${form.gradientColor2})`
        : `linear-gradient(${form.gradientDirection}, ${form.gradientColor1}, ${form.gradientColor2})`
      break
    case 'image':
      bg = form.imageData
        ? `url(${form.imageData}) center/cover no-repeat`
        : form.solidColor
      break
    default:
      bg = form.solidColor
  }
  return { background: bg }
})

watch(() => props.modelValue, (visible) => {
  if (visible) {
    Object.assign(form, { ...themeSettings.value })
  }
})

const handleImageSelect = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  imageSizeMB.value = +(file.size / 1024 / 1024).toFixed(1)
  const reader = new FileReader()
  reader.onload = () => { form.imageData = reader.result }
  reader.readAsDataURL(file)
}

const handleApply = () => {
  const settings = { ...form }
  // 径向渐变特殊处理
  if (settings.mode === 'gradient' && settings.gradientDirection === 'circle') {
    // applyToDOM will handle this via radial-gradient
  }
  applyTheme(settings)
  emit('update:modelValue', false)
}

const handleReset = () => {
  resetTheme()
  emit('update:modelValue', false)
}
</script>

<style scoped>
.theme-section {
  margin-bottom: 18px;
}
.theme-label {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}
.theme-label .hint {
  font-weight: 400;
  color: #999;
  font-size: 12px;
}
.gradient-colors {
  display: flex;
  align-items: center;
  gap: 12px;
}
.arrow {
  color: #999;
  font-size: 18px;
}
.image-upload {
  display: flex;
  align-items: center;
  gap: 10px;
}
.image-info {
  font-size: 12px;
  color: #67c23a;
}
.image-info.hint {
  color: #999;
}
.size-warning {
  margin-top: 6px;
  font-size: 12px;
  color: #e6a23c;
}
.preview-box {
  width: 100%;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  border: 1px solid #eee;
}
.preview-overlay {
  position: absolute;
  inset: 0;
  background: #fff;
}
.preview-text {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
</style>
