<template>
  <div class="effect-rule-manager">
    <div class="header">
      <h2>彩蛋规则管理</h2>
      <el-button type="primary" @click="openCreateDialog">新增规则</el-button>
    </div>

    <!-- 表格列表 -->
    <el-table :data="rules" border stripe style="width: 100%">
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="effectType" label="特效类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getEffectTypeTagType(row.effectType)">
            {{ getEffectTypeLabel(row.effectType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="触发条件" min-width="200">
        <template #default="{ row }">
          <div v-if="row.exactAmount !== null">
            金额 = {{ row.exactAmount }}
          </div>
          <div v-else-if="row.minAmount !== null || row.maxAmount !== null">
            {{ row.minAmount ?? '∞' }} ≤ 金额 ≤ {{ row.maxAmount ?? '∞' }}
          </div>
          <div v-else>-</div>
          <div v-if="row.transactionType" class="sub-condition">
            类型: {{ getTransactionTypeLabel(row.transactionType) }}
          </div>
          <div v-if="row.categoryId" class="sub-condition">
            分类ID: {{ row.categoryId }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="100" />
      <el-table-column prop="enabled" label="启用状态" width="100">
        <template #default="{ row }">
          <el-switch
            v-model="row.enabled"
            @change="handleToggleEnabled(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openPreview(row)">预览</el-button>
          <el-button size="small" type="primary" @click="openEditDialog(row)">
            编辑
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑规则' : '新增规则'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入规则名称" />
        </el-form-item>

        <el-form-item label="特效类型" prop="effectType">
          <el-select
            v-model="formData.effectType"
            placeholder="请选择特效类型"
            style="width: 100%"
          >
            <el-option label="烟花" value="FIREWORKS" />
            <el-option label="爱心" value="HEARTS" />
            <el-option label="金币雨" value="GOLD_RAIN" />
            <el-option label="视频" value="VIDEO" />
          </el-select>
        </el-form-item>

        <el-form-item
          v-if="formData.effectType === 'VIDEO'"
          label="视频文件"
          prop="videoFilename"
        >
          <el-upload
            :auto-upload="false"
            :on-change="handleVideoChange"
            accept="video/*"
            :limit="1"
          >
            <el-button type="primary">选择视频文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持 mp4、webm 等格式，文件大小不超过 50MB
              </div>
            </template>
          </el-upload>
          <div v-if="formData.videoFilename" class="video-filename">
            已选择: {{ formData.videoFilename }}
          </div>
        </el-form-item>

        <el-form-item label="交易类型">
          <el-select
            v-model="formData.transactionType"
            placeholder="不限"
            clearable
            style="width: 100%"
          >
            <el-option label="支出" value="EXPENSE" />
            <el-option label="收入" value="INCOME" />
          </el-select>
        </el-form-item>

        <el-form-item label="精确金额">
          <el-input-number
            v-model="formData.exactAmount"
            :min="0"
            :precision="2"
            placeholder="如 520、1314"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="金额范围">
          <div class="amount-range">
            <el-input-number
              v-model="formData.minAmount"
              :min="0"
              :precision="2"
              placeholder="最小金额"
              style="width: 45%"
            />
            <span class="range-separator">至</span>
            <el-input-number
              v-model="formData.maxAmount"
              :min="0"
              :precision="2"
              placeholder="最大金额"
              style="width: 45%"
            />
          </div>
        </el-form-item>

        <el-form-item label="分类 ID">
          <el-input-number
            v-model="formData.categoryId"
            :min="1"
            placeholder="可选"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="优先级">
          <el-input-number
            v-model="formData.priority"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="启用状态">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 视频预览弹窗 -->
    <el-dialog v-model="videoPreviewVisible" title="视频预览" width="800px">
      <video
        v-if="previewVideoUrl"
        :src="previewVideoUrl"
        controls
        autoplay
        style="width: 100%"
      />
    </el-dialog>

    <!-- 预览动画容器 -->
    <div
      v-if="previewContainerVisible"
      class="preview-container"
      @click="closePreview"
    >
      <!-- 烟花动画 Canvas -->
      <canvas
        v-if="previewEffectType === 'FIREWORKS'"
        ref="fireworksCanvas"
        class="fireworks-canvas"
      />

      <!-- 爱心飘落 -->
      <div v-if="previewEffectType === 'HEARTS'" class="hearts-container">
        <div
          v-for="heart in hearts"
          :key="heart.id"
          class="heart"
          :style="{
            left: heart.left + '%',
            animationDuration: heart.duration + 's',
            animationDelay: heart.delay + 's'
          }"
        >
          ❤️
        </div>
      </div>

      <!-- 金币雨 -->
      <div v-if="previewEffectType === 'GOLD_RAIN'" class="gold-rain-container">
        <div
          v-for="coin in coins"
          :key="coin.id"
          class="coin"
          :style="{
            left: coin.left + '%',
            animationDuration: coin.duration + 's',
            animationDelay: coin.delay + 's'
          }"
        >
          🪙
        </div>
      </div>

      <div class="preview-hint">点击任意位置关闭预览</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getEffectRules,
  createEffectRule,
  updateEffectRule,
  deleteEffectRule,
  uploadEffectVideo
} from '../../api/index.js'

// 数据
const rules = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

// 表单数据
const formData = reactive({
  id: null,
  name: '',
  effectType: '',
  videoFilename: null,
  transactionType: null,
  exactAmount: null,
  minAmount: null,
  maxAmount: null,
  categoryId: null,
  priority: 0,
  enabled: true
})

// 表单验证规则
const formRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  effectType: [{ required: true, message: '请选择特效类型', trigger: 'change' }]
}

// 预览相关
const previewContainerVisible = ref(false)
const previewEffectType = ref('')
const previewVideoUrl = ref('')
const videoPreviewVisible = ref(false)
const fireworksCanvas = ref(null)
const hearts = ref([])
const coins = ref([])

// 获取特效类型标签颜色
const getEffectTypeTagType = (type) => {
  const map = {
    FIREWORKS: 'danger',
    HEARTS: 'danger',
    GOLD_RAIN: 'warning',
    VIDEO: 'primary'
  }
  return map[type] || ''
}

// 获取特效类型标签文本
const getEffectTypeLabel = (type) => {
  const map = {
    FIREWORKS: '烟花',
    HEARTS: '爱心',
    GOLD_RAIN: '金币雨',
    VIDEO: '视频'
  }
  return map[type] || type
}

// 获取交易类型标签
const getTransactionTypeLabel = (type) => {
  const map = {
    EXPENSE: '支出',
    INCOME: '收入'
  }
  return map[type] || type
}

// 加载规则列表
const loadRules = async () => {
  try {
    const res = await getEffectRules()
    rules.value = res.data
  } catch (error) {
    ElMessage.error('加载规则列表失败')
  }
}

// 打开新增对话框
const openCreateDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 打开编辑对话框
const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    effectType: row.effectType,
    videoFilename: row.videoFilename,
    transactionType: row.transactionType,
    exactAmount: row.exactAmount,
    minAmount: row.minAmount,
    maxAmount: row.maxAmount,
    categoryId: row.categoryId,
    priority: row.priority,
    enabled: row.enabled
  })
  dialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    id: null,
    name: '',
    effectType: '',
    videoFilename: null,
    transactionType: null,
    exactAmount: null,
    minAmount: null,
    maxAmount: null,
    categoryId: null,
    priority: 0,
    enabled: true
  })
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 处理视频文件选择
const handleVideoChange = async (file) => {
  if (!file.raw) return

  const maxSize = 50 * 1024 * 1024 // 50MB
  if (file.raw.size > maxSize) {
    ElMessage.error('视频文件大小不能超过 50MB')
    return
  }

  try {
    ElMessage.info('正在上传视频...')
    const res = await uploadEffectVideo(file.raw)
    formData.videoFilename = res.data.filename
    ElMessage.success('视频上传成功')
  } catch (error) {
    ElMessage.error('视频上传失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const data = {
        name: formData.name,
        effectType: formData.effectType,
        videoFilename: formData.videoFilename,
        transactionType: formData.transactionType || null,
        exactAmount: formData.exactAmount,
        minAmount: formData.minAmount,
        maxAmount: formData.maxAmount,
        categoryId: formData.categoryId,
        priority: formData.priority,
        enabled: formData.enabled
      }

      if (isEdit.value) {
        await updateEffectRule(formData.id, data)
        ElMessage.success('更新成功')
      } else {
        await createEffectRule(data)
        ElMessage.success('创建成功')
      }

      dialogVisible.value = false
      await loadRules()
    } catch (error) {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    } finally {
      submitting.value = false
    }
  })
}

// 切换启用状态
const handleToggleEnabled = async (row) => {
  try {
    await updateEffectRule(row.id, {
      ...row,
      enabled: row.enabled
    })
    ElMessage.success('状态更新成功')
  } catch (error) {
    // 恢复原状态
    row.enabled = !row.enabled
    ElMessage.error('状态更新失败')
  }
}

// 删除规则
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除规则"${row.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteEffectRule(row.id)
    ElMessage.success('删除成功')
    await loadRules()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 打开预览
const openPreview = (row) => {
  previewEffectType.value = row.effectType

  if (row.effectType === 'VIDEO') {
    if (row.videoFilename) {
      previewVideoUrl.value = `/api/effect-videos/${row.videoFilename}`
      videoPreviewVisible.value = true
    } else {
      ElMessage.warning('该规则未配置视频文件')
    }
    return
  }

  previewContainerVisible.value = true

  if (row.effectType === 'FIREWORKS') {
    nextTick(() => {
      startFireworksAnimation()
    })
  } else if (row.effectType === 'HEARTS') {
    generateHearts()
  } else if (row.effectType === 'GOLD_RAIN') {
    generateCoins()
  }

  // 3秒后自动关闭
  setTimeout(() => {
    closePreview()
  }, 3000)
}

// 关闭预览
const closePreview = () => {
  previewContainerVisible.value = false
  previewEffectType.value = ''
  hearts.value = []
  coins.value = []
}

// 生成爱心
const generateHearts = () => {
  const count = 30
  hearts.value = Array.from({ length: count }, (_, i) => ({
    id: i,
    left: Math.random() * 100,
    duration: 2 + Math.random() * 2,
    delay: Math.random() * 1
  }))
}

// 生成金币
const generateCoins = () => {
  const count = 30
  coins.value = Array.from({ length: count }, (_, i) => ({
    id: i,
    left: Math.random() * 100,
    duration: 2 + Math.random() * 2,
    delay: Math.random() * 1
  }))
}

// 烟花动画
const startFireworksAnimation = () => {
  const canvas = fireworksCanvas.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const particles = []
  const colors = ['#ff0000', '#ff6600', '#ffff00', '#00ff00', '#0066ff', '#ff00ff']

  // 创建粒子
  const createParticles = (x, y) => {
    const particleCount = 50
    for (let i = 0; i < particleCount; i++) {
      const angle = (Math.PI * 2 * i) / particleCount
      const velocity = 2 + Math.random() * 3
      particles.push({
        x,
        y,
        vx: Math.cos(angle) * velocity,
        vy: Math.sin(angle) * velocity,
        alpha: 1,
        color: colors[Math.floor(Math.random() * colors.length)],
        size: 2 + Math.random() * 2
      })
    }
  }

  // 发射烟花
  const launchFirework = () => {
    const x = Math.random() * canvas.width
    const targetY = canvas.height * 0.2 + Math.random() * canvas.height * 0.3
    createParticles(x, targetY)
  }

  // 初始发射几个烟花
  for (let i = 0; i < 3; i++) {
    setTimeout(() => launchFirework(), i * 300)
  }

  // 动画循环
  const animate = () => {
    if (!previewContainerVisible.value) return

    ctx.fillStyle = 'rgba(0, 0, 0, 0.1)'
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    for (let i = particles.length - 1; i >= 0; i--) {
      const p = particles[i]
      p.x += p.vx
      p.y += p.vy
      p.vy += 0.05 // 重力
      p.alpha -= 0.015

      if (p.alpha <= 0) {
        particles.splice(i, 1)
        continue
      }

      ctx.globalAlpha = p.alpha
      ctx.fillStyle = p.color
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
      ctx.fill()
    }

    ctx.globalAlpha = 1
    requestAnimationFrame(animate)
  }

  animate()
}

// 初始化
onMounted(() => {
  loadRules()
})
</script>

<style scoped>
.effect-rule-manager {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
}

.sub-condition {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.amount-range {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.range-separator {
  color: #909399;
}

.video-filename {
  margin-top: 8px;
  font-size: 12px;
  color: #67c23a;
}

/* 预览容器 */
.preview-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  pointer-events: none;
  background: rgba(0, 0, 0, 0.3);
}

.fireworks-canvas {
  width: 100%;
  height: 100%;
}

.hearts-container,
.gold-rain-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.heart,
.coin {
  position: absolute;
  top: -50px;
  font-size: 30px;
  animation: fall linear forwards;
  pointer-events: auto;
}

@keyframes fall {
  to {
    transform: translateY(calc(100vh + 50px)) rotate(360deg);
    opacity: 0;
  }
}

.preview-hint {
  position: absolute;
  bottom: 50px;
  left: 50%;
  transform: translateX(-50%);
  color: white;
  font-size: 16px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
  pointer-events: auto;
}
</style>
