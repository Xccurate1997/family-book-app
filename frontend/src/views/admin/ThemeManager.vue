<template>
  <div class="theme-manager">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>主题管理</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新增主题
      </el-button>
    </div>

    <!-- 主题列表 -->
    <div class="theme-grid">
      <el-card
        v-for="theme in themes"
        :key="theme.id"
        class="theme-card"
        :class="{ 'default-theme': theme.isDefault }"
      >
        <template #header>
          <div class="card-header">
            <span class="theme-name">{{ theme.name }}</span>
            <el-tag v-if="theme.isDefault" type="success" size="small">默认</el-tag>
          </div>
        </template>

        <div class="card-content">
          <p class="theme-description">{{ theme.description || '暂无描述' }}</p>
          
          <div class="color-preview">
            <span>主色调：</span>
            <div
              class="color-block"
              :style="{ backgroundColor: theme.primaryColor }"
            ></div>
            <span class="color-value">{{ theme.primaryColor }}</span>
          </div>
        </div>

        <div class="card-actions">
          <el-button size="small" @click="openEditDialog(theme)">编辑</el-button>
          <el-button size="small" @click="openAssignDialog(theme)">分配用户</el-button>
          <el-button
            size="small"
            type="danger"
            :disabled="theme.isDefault"
            @click="handleDelete(theme)"
          >
            删除
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 新增/编辑主题弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑主题' : '新增主题'"
      :width="windowWidth < 640 ? '95%' : '700px'"
      @close="resetForm"
    >
      <el-form :model="formData" label-width="120px">
        <el-tabs v-model="activeTab">
          <!-- 基础信息 Tab -->
          <el-tab-pane label="基础信息" name="basic">
            <el-form-item label="主题名称" required>
              <el-input v-model="formData.name" placeholder="请输入主题名称" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input
                v-model="formData.description"
                type="textarea"
                :rows="3"
                placeholder="请输入主题描述"
              />
            </el-form-item>
            <el-form-item label="是否默认主题">
              <el-switch v-model="formData.isDefault" />
            </el-form-item>
            <el-form-item label="主色调">
              <el-color-picker v-model="formData.primaryColor" />
            </el-form-item>
            <el-form-item label="顶栏背景色">
              <el-color-picker v-model="formData.headerBg" />
            </el-form-item>
          </el-tab-pane>

          <!-- 开屏动画 Tab -->
          <el-tab-pane label="开屏动画" name="splash">
            <el-form-item label="开屏类型">
              <el-select v-model="formData.splashType" placeholder="请选择开屏类型">
                <el-option label="无" value="NONE" />
                <el-option label="淡入" value="FADE_IN" />
                <el-option label="上滑" value="SLIDE_UP" />
                <el-option label="自定义图片" value="CUSTOM_IMAGE" />
              </el-select>
            </el-form-item>
            <el-form-item
              v-if="formData.splashType === 'CUSTOM_IMAGE'"
              label="开屏图片"
            >
              <el-upload
                :show-file-list="false"
                :before-upload="(file) => handleImageUpload(file, 'splash')"
                accept="image/*"
              >
                <el-button size="small">上传图片</el-button>
              </el-upload>
              <div v-if="formData.splashImageFilename" class="image-preview">
                <img
                  :src="getImageUrl(formData.splashImageFilename)"
                  alt="开屏图片预览"
                />
              </div>
            </el-form-item>
            <el-form-item label="开屏持续时间（毫秒）">
              <el-input-number
                v-model="formData.splashDuration"
                :min="500"
                :max="10000"
                :step="100"
              />
            </el-form-item>
          </el-tab-pane>

          <!-- 音效 Tab -->
          <el-tab-pane label="音效" name="sound">
            <el-form-item label="音效文件">
              <el-upload
                :show-file-list="false"
                :before-upload="handleSoundUpload"
                accept=".mp3,.ogg,.wav"
              >
                <el-button size="small">上传音效</el-button>
              </el-upload>
              <div v-if="formData.soundFilename" class="sound-info">
                <span>{{ formData.soundFilename }}</span>
                <audio :src="getSoundUrl(formData.soundFilename)" controls />
              </div>
            </el-form-item>
          </el-tab-pane>

          <!-- 装饰图片 Tab -->
          <el-tab-pane label="装饰图片" name="decorations">
            <el-tabs v-model="decorationTab" type="border-card">
              <el-tab-pane
                v-for="tab in decorationTabs"
                :key="tab.key"
                :label="tab.label"
                :name="tab.key"
              >
                <div class="decoration-images">
                  <div class="image-upload-group">
                    <label>左侧图片：</label>
                    <el-upload
                      :show-file-list="false"
                      :before-upload="
                        (file) => handleDecorationUpload(file, tab.key, 'left')
                      "
                      accept="image/*"
                    >
                      <el-button size="small">上传</el-button>
                    </el-upload>
                    <div
                      v-if="getDecoration(tab.key, 'left')"
                      class="decoration-preview"
                    >
                      <img
                        :src="getImageUrl(getDecoration(tab.key, 'left'))"
                        alt="左侧图片"
                      />
                    </div>
                  </div>
                  <div class="image-upload-group">
                    <label>右侧图片：</label>
                    <el-upload
                      :show-file-list="false"
                      :before-upload="
                        (file) => handleDecorationUpload(file, tab.key, 'right')
                      "
                      accept="image/*"
                    >
                      <el-button size="small">上传</el-button>
                    </el-upload>
                    <div
                      v-if="getDecoration(tab.key, 'right')"
                      class="decoration-preview"
                    >
                      <img
                        :src="getImageUrl(getDecoration(tab.key, 'right'))"
                        alt="右侧图片"
                      />
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </el-tab-pane>

          <!-- 点缀配置 Tab -->
          <el-tab-pane label="点缀配置" name="accents">
            <div class="accent-item">
              <div class="accent-label">Header Logo/吉祥物</div>
              <div class="accent-desc">显示在标题左侧的小图标/吉祥物，建议 64x64px</div>
              <div class="accent-upload">
                <el-button size="small" @click="handleUploadAccent('logoFilename')">上传图片</el-button>
                <el-button v-if="formData.logoFilename" size="small" type="danger" text @click="formData.logoFilename = null">清除</el-button>
              </div>
              <div v-if="formData.logoFilename" class="accent-preview">
                <img :src="`/api/theme-assets/${formData.logoFilename}`" style="width: 64px; height: 64px; object-fit: contain;" />
              </div>
            </div>

            <div class="accent-item">
              <div class="accent-label">Header 背景纹理</div>
              <div class="accent-desc">Header 背景纹理图片，会以低透明度平铺</div>
              <div class="accent-upload">
                <el-button size="small" @click="handleUploadAccent('headerPatternFilename')">上传图片</el-button>
                <el-button v-if="formData.headerPatternFilename" size="small" type="danger" text @click="formData.headerPatternFilename = null">清除</el-button>
              </div>
              <div v-if="formData.headerPatternFilename" class="accent-preview">
                <img :src="`/api/theme-assets/${formData.headerPatternFilename}`" style="width: 200px; height: 40px; object-fit: contain;" />
              </div>
              <div class="accent-slider">
                <span class="slider-label">纹理透明度：{{ (formData.headerPatternOpacity * 100).toFixed(0) }}%</span>
                <el-slider v-model="formData.headerPatternOpacity" :min="0" :max="1" :step="0.01" style="width: 300px;" />
              </div>
            </div>

            <div class="accent-item">
              <div class="accent-label">底部角落装饰</div>
              <div class="accent-desc">显示在页面左下角和右下角的装饰图片/GIF，建议 80x80px</div>
              <div class="accent-upload">
                <el-button size="small" @click="handleUploadAccent('cornerDecoFilename')">上传图片</el-button>
                <el-button v-if="formData.cornerDecoFilename" size="small" type="danger" text @click="formData.cornerDecoFilename = null">清除</el-button>
              </div>
              <div v-if="formData.cornerDecoFilename" class="accent-preview">
                <img :src="`/api/theme-assets/${formData.cornerDecoFilename}`" style="width: 80px; height: 80px; object-fit: contain;" />
              </div>
            </div>

            <div class="accent-item">
              <div class="accent-label">空状态插图</div>
              <div class="accent-desc">账单列表为空时显示的插图，建议 200x200px</div>
              <div class="accent-upload">
                <el-button size="small" @click="handleUploadAccent('emptyStateFilename')">上传图片</el-button>
                <el-button v-if="formData.emptyStateFilename" size="small" type="danger" text @click="formData.emptyStateFilename = null">清除</el-button>
              </div>
              <div v-if="formData.emptyStateFilename" class="accent-preview">
                <img :src="`/api/theme-assets/${formData.emptyStateFilename}`" style="width: 200px; height: 200px; object-fit: contain;" />
              </div>
            </div>

            <el-divider />

            <div class="accent-item">
              <div class="accent-label">收入记录背景图</div>
              <div class="accent-desc">每条收入记录行的背景图片，会以半透明叠加显示</div>
              <div class="accent-upload">
                <el-button size="small" @click="handleUploadAccent('incomeBgFilename')">上传图片</el-button>
                <el-button v-if="formData.incomeBgFilename" size="small" type="danger" text @click="formData.incomeBgFilename = null">清除</el-button>
              </div>
              <div v-if="formData.incomeBgFilename" class="accent-preview">
                <img :src="`/api/theme-assets/${formData.incomeBgFilename}`" style="width: 200px; height: 48px; object-fit: cover; border-radius: 10px;" />
              </div>
            </div>

            <div class="accent-item">
              <div class="accent-label">支出记录背景图</div>
              <div class="accent-desc">每条支出记录行的背景图片，会以半透明叠加显示</div>
              <div class="accent-upload">
                <el-button size="small" @click="handleUploadAccent('expenseBgFilename')">上传图片</el-button>
                <el-button v-if="formData.expenseBgFilename" size="small" type="danger" text @click="formData.expenseBgFilename = null">清除</el-button>
              </div>
              <div v-if="formData.expenseBgFilename" class="accent-preview">
                <img :src="`/api/theme-assets/${formData.expenseBgFilename}`" style="width: 200px; height: 48px; object-fit: cover; border-radius: 10px;" />
              </div>
            </div>
          </el-tab-pane>

          <!-- 装饰参数 Tab -->
          <el-tab-pane label="装饰参数" name="decoParams">
            <div class="deco-param-section">
              <h4>两侧装饰图片参数</h4>
              <p class="param-desc">调整 Tab 页面两侧装饰图片的显示效果</p>

              <el-form-item label="图片透明度">
                <div class="slider-row">
                  <el-slider v-model="formData.decoOpacity" :min="0" :max="1" :step="0.05" style="flex: 1;" />
                  <span class="slider-value">{{ (formData.decoOpacity * 100).toFixed(0) }}%</span>
                </div>
              </el-form-item>

              <el-form-item label="最大高度 (vh)">
                <div class="slider-row">
                  <el-slider v-model="formData.decoMaxHeight" :min="20" :max="100" :step="5" style="flex: 1;" />
                  <span class="slider-value">{{ formData.decoMaxHeight }}vh</span>
                </div>
              </el-form-item>

              <el-form-item label="最大宽度 (px)">
                <div class="slider-row">
                  <el-slider v-model="formData.decoMaxWidth" :min="80" :max="500" :step="10" style="flex: 1;" />
                  <span class="slider-value">{{ formData.decoMaxWidth }}px</span>
                </div>
              </el-form-item>

              <el-form-item label="上下渐变边缘">
                <div class="slider-row">
                  <el-slider v-model="formData.decoFadeEdge" :min="0" :max="50" :step="1" style="flex: 1;" />
                  <span class="slider-value">{{ formData.decoFadeEdge }}%</span>
                </div>
              </el-form-item>

              <el-form-item label="左右渐变范围">
                <div class="slider-row">
                  <el-slider v-model="formData.decoFadeSide" :min="0" :max="100" :step="5" style="flex: 1;" />
                  <span class="slider-value">{{ formData.decoFadeSide }}%</span>
                </div>
              </el-form-item>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 用户分配弹窗 -->
    <el-dialog
      v-model="assignDialogVisible"
      title="分配用户"
      :width="windowWidth < 640 ? '92%' : '500px'"
    >
      <div class="assign-content">
        <div class="assigned-users">
          <h4>已分配用户：</h4>
          <el-tag
            v-for="user in assignedUsers"
            :key="user.username"
            closable
            @close="handleRemoveUser(user.username)"
            class="user-tag"
          >
            {{ user.nickname || user.username }}（{{ user.username }}）
          </el-tag>
          <p v-if="assignedUsers.length === 0" class="empty-tip">暂无分配用户</p>
        </div>

        <div class="add-user">
          <el-input
            v-model="newUsername"
            placeholder="输入用户名"
            style="width: 200px; margin-right: 10px"
          />
          <el-button type="primary" @click="handleAddUser">添加</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getAllThemes,
  createTheme,
  updateTheme,
  deleteTheme,
  getThemeAssignments,
  assignThemeToUser,
  removeThemeAssignment,
  uploadThemeImage,
  uploadThemeSound
} from '../../api/index.js'

// 窗口宽度响应式
const windowWidth = ref(window.innerWidth)
const onResize = () => { windowWidth.value = window.innerWidth }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

// 主题列表
const themes = ref([])

// 弹窗状态
const dialogVisible = ref(false)
const assignDialogVisible = ref(false)
const isEdit = ref(false)
const activeTab = ref('basic')
const decorationTab = ref('transactions')

// 表单数据
const formData = reactive({
  id: null,
  name: '',
  description: '',
  isDefault: false,
  primaryColor: '#409eff',
  headerBg: '#ecf5ff',
  splashType: 'NONE',
  splashImageFilename: null,
  splashDuration: 2000,
  soundFilename: null,
  decorations: {},
  logoFilename: null,
  headerPatternFilename: null,
  headerPatternOpacity: 0.06,
  cornerDecoFilename: null,
  emptyStateFilename: null,
  decoOpacity: 0.6,
  decoMaxHeight: 60,
  decoMaxWidth: 280,
  decoFadeEdge: 15,
  decoFadeSide: 70,
  incomeBgFilename: null,
  expenseBgFilename: null
})

// 装饰图片 Tab 配置
const decorationTabs = [
  { key: 'transactions', label: '账单' },
  { key: 'charts', label: '图表' },
  { key: 'yearly', label: '年报' },
  { key: 'calendar', label: '日历' },
  { key: 'categories', label: '分类' }
]

// 用户分配相关
const currentThemeId = ref(null)
const assignedUsers = ref([])
const newUsername = ref('')

// 初始化装饰数据结构
const initDecorations = () => {
  const decorations = {}
  decorationTabs.forEach((tab) => {
    decorations[tab.key] = {
      leftImageFilename: null,
      rightImageFilename: null
    }
  })
  return decorations
}

// 获取装饰图片文件名
const getDecoration = (tabKey, side) => {
  if (!formData.decorations[tabKey]) {
    return null
  }
  return side === 'left'
    ? formData.decorations[tabKey].leftImageFilename
    : formData.decorations[tabKey].rightImageFilename
}

// 设置装饰图片文件名
const setDecoration = (tabKey, side, filename) => {
  if (!formData.decorations[tabKey]) {
    formData.decorations[tabKey] = {
      leftImageFilename: null,
      rightImageFilename: null
    }
  }
  if (side === 'left') {
    formData.decorations[tabKey].leftImageFilename = filename
  } else {
    formData.decorations[tabKey].rightImageFilename = filename
  }
}

// 获取图片 URL
const getImageUrl = (filename) => {
  if (!filename) return ''
  return `/api/theme-assets/${filename}`
}

// 获取音效 URL
const getSoundUrl = (filename) => {
  if (!filename) return ''
  return `/api/theme-assets/${filename}`
}

// 加载主题列表
const loadThemes = async () => {
  try {
    const response = await getAllThemes()
    themes.value = response.data
  } catch (error) {
    ElMessage.error('加载主题列表失败')
    console.error(error)
  }
}

// 打开新增弹窗
const openCreateDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 打开编辑弹窗
const openEditDialog = (theme) => {
  isEdit.value = true
  formData.id = theme.id
  formData.name = theme.name
  formData.description = theme.description || ''
  formData.isDefault = theme.isDefault
  formData.primaryColor = theme.primaryColor
  formData.headerBg = theme.headerBg
  formData.splashType = theme.splashType || 'NONE'
  formData.splashImageFilename = theme.splashImageFilename || null
  formData.splashDuration = theme.splashDuration || 2000
  formData.soundFilename = theme.soundFilename || null
  formData.logoFilename = theme.logoFilename || null
  formData.headerPatternFilename = theme.headerPatternFilename || null
  formData.headerPatternOpacity = theme.headerPatternOpacity ?? 0.06
  formData.cornerDecoFilename = theme.cornerDecoFilename || null
  formData.emptyStateFilename = theme.emptyStateFilename || null
  formData.decoOpacity = theme.decoOpacity ?? 0.6
  formData.decoMaxHeight = theme.decoMaxHeight ?? 60
  formData.decoMaxWidth = theme.decoMaxWidth ?? 280
  formData.decoFadeEdge = theme.decoFadeEdge ?? 15
  formData.decoFadeSide = theme.decoFadeSide ?? 70
  formData.incomeBgFilename = theme.incomeBgFilename || null
  formData.expenseBgFilename = theme.expenseBgFilename || null

  // 将 decorations 数组转换为按 tabKey 索引的对象
  formData.decorations = initDecorations()
  if (theme.decorations && Array.isArray(theme.decorations)) {
    theme.decorations.forEach((dec) => {
      if (formData.decorations[dec.tabKey]) {
        formData.decorations[dec.tabKey].leftImageFilename =
          dec.leftImageFilename || null
        formData.decorations[dec.tabKey].rightImageFilename =
          dec.rightImageFilename || null
      }
    })
  }

  dialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  formData.id = null
  formData.name = ''
  formData.description = ''
  formData.isDefault = false
  formData.primaryColor = '#409eff'
  formData.headerBg = '#ecf5ff'
  formData.splashType = 'NONE'
  formData.splashImageFilename = null
  formData.splashDuration = 2000
  formData.soundFilename = null
  formData.decorations = initDecorations()
  formData.logoFilename = null
  formData.headerPatternFilename = null
  formData.headerPatternOpacity = 0.06
  formData.cornerDecoFilename = null
  formData.emptyStateFilename = null
  formData.decoOpacity = 0.6
  formData.decoMaxHeight = 60
  formData.decoMaxWidth = 280
  formData.decoFadeEdge = 15
  formData.decoFadeSide = 70
  formData.incomeBgFilename = null
  formData.expenseBgFilename = null
  activeTab.value = 'basic'
  decorationTab.value = 'transactions'
}

// 提交表单
const handleSubmit = async () => {
  if (!formData.name) {
    ElMessage.warning('请输入主题名称')
    return
  }

  try {
    // 将 decorations 对象转换回数组格式
    const decorationsArray = []
    Object.keys(formData.decorations).forEach((tabKey) => {
      const dec = formData.decorations[tabKey]
      if (dec.leftImageFilename || dec.rightImageFilename) {
        decorationsArray.push({
          tabKey,
          leftImageFilename: dec.leftImageFilename || null,
          rightImageFilename: dec.rightImageFilename || null
        })
      }
    })

    const data = {
      name: formData.name,
      description: formData.description,
      isDefault: formData.isDefault,
      primaryColor: formData.primaryColor,
      headerBg: formData.headerBg,
      splashType: formData.splashType,
      splashImageFilename: formData.splashImageFilename,
      splashDuration: formData.splashDuration,
      soundFilename: formData.soundFilename,
      decorations: decorationsArray,
      logoFilename: formData.logoFilename,
      headerPatternFilename: formData.headerPatternFilename,
      headerPatternOpacity: formData.headerPatternOpacity,
      cornerDecoFilename: formData.cornerDecoFilename,
      emptyStateFilename: formData.emptyStateFilename,
      decoOpacity: formData.decoOpacity,
      decoMaxHeight: formData.decoMaxHeight,
      decoMaxWidth: formData.decoMaxWidth,
      decoFadeEdge: formData.decoFadeEdge,
      decoFadeSide: formData.decoFadeSide,
      incomeBgFilename: formData.incomeBgFilename,
      expenseBgFilename: formData.expenseBgFilename
    }

    if (isEdit.value) {
      await updateTheme(formData.id, data)
      ElMessage.success('更新成功')
    } else {
      await createTheme(data)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadThemes()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    console.error(error)
  }
}

// 删除主题
const handleDelete = async (theme) => {
  if (theme.isDefault) {
    ElMessage.warning('默认主题不可删除')
    return
  }

  try {
    await ElMessageBox.confirm('确定要删除该主题吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteTheme(theme.id)
    ElMessage.success('删除成功')
    loadThemes()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }
}

// 图片上传处理
const handleImageUpload = async (file, type) => {
  try {
    const response = await uploadThemeImage(file)
    const filename = response.data.filename

    if (type === 'splash') {
      formData.splashImageFilename = filename
    }

    ElMessage.success('上传成功')
    return false // 阻止默认上传行为
  } catch (error) {
    ElMessage.error('上传失败')
    console.error(error)
    return false
  }
}

// 音效上传处理
const handleSoundUpload = async (file) => {
  try {
    const response = await uploadThemeSound(file)
    formData.soundFilename = response.data.filename
    ElMessage.success('上传成功')
    return false
  } catch (error) {
    ElMessage.error('上传失败')
    console.error(error)
    return false
  }
}

// 装饰图片上传处理
const handleDecorationUpload = async (file, tabKey, side) => {
  try {
    const response = await uploadThemeImage(file)
    const filename = response.data.filename
    setDecoration(tabKey, side, filename)
    ElMessage.success('上传成功')
    return false
  } catch (error) {
    ElMessage.error('上传失败')
    console.error(error)
    return false
  }
}

// 点缀配置上传处理
const handleUploadAccent = async (fieldName) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    try {
      const response = await uploadThemeImage(file)
      formData[fieldName] = response.data.filename
      ElMessage.success('上传成功')
    } catch (error) {
      ElMessage.error('上传失败')
      console.error(error)
    }
  }
  input.click()
}

// 打开用户分配弹窗
const openAssignDialog = async (theme) => {
  currentThemeId.value = theme.id
  assignedUsers.value = []
  newUsername.value = ''

  try {
    const response = await getThemeAssignments(theme.id)
    assignedUsers.value = response.data.map((item) => ({
      username: item.username || `user_${item.userId}`,
      nickname: item.nickname || ''
    }))
  } catch (error) {
    ElMessage.error('加载分配用户失败')
    console.error(error)
  }

  assignDialogVisible.value = true
}

// 添加用户
const handleAddUser = async () => {
  if (!newUsername.value) {
    ElMessage.warning('请输入用户名')
    return
  }

  try {
    await assignThemeToUser(currentThemeId.value, newUsername.value)
    ElMessage.success('添加成功')
    assignedUsers.value.push({ username: newUsername.value, nickname: '' })
    newUsername.value = ''
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '添加失败，请检查用户名是否正确')
    console.error(error)
  }
}

// 移除用户
const handleRemoveUser = async (username) => {
  try {
    await removeThemeAssignment(currentThemeId.value, username)
    ElMessage.success('移除成功')
    assignedUsers.value = assignedUsers.value.filter((u) => u.username !== username)
  } catch (error) {
    ElMessage.error('移除失败')
    console.error(error)
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadThemes()
})
</script>

<style scoped>
.theme-manager {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
@media (max-width: 900px) {
  .theme-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}
@media (max-width: 640px) {
  .theme-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .theme-manager {
    padding: 12px;
  }
  .decoration-images {
    flex-direction: column;
    gap: 12px;
  }
  .add-user .el-input {
    width: 100% !important;
    margin-right: 0 !important;
    margin-bottom: 8px;
  }
  .add-user {
    flex-direction: column;
    align-items: stretch;
  }
}

.theme-card {
  transition: all 0.3s;
}

.theme-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.theme-card.default-theme {
  border: 2px solid #67c23a;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.theme-name {
  font-weight: bold;
  font-size: 16px;
}

.card-content {
  min-height: 100px;
}

.theme-description {
  color: #606266;
  margin-bottom: 15px;
}

.color-preview {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-block {
  width: 30px;
  height: 30px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.color-value {
  font-family: monospace;
  font-size: 12px;
  color: #909399;
}

.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 15px;
}

.image-preview {
  margin-top: 10px;
}

.image-preview img {
  max-width: 200px;
  max-height: 150px;
  border-radius: 4px;
}

.sound-info {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.decoration-images {
  display: flex;
  gap: 20px;
}

.image-upload-group {
  flex: 1;
}

.image-upload-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
}

.decoration-preview {
  margin-top: 10px;
}

.decoration-preview img {
  width: 120px;
  height: 180px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.accent-item {
  margin-bottom: 20px;
}

.accent-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.accent-desc {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.accent-upload {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.accent-preview {
  border: 1px dashed #ddd;
  border-radius: 6px;
  padding: 8px;
  display: inline-block;
  background: #fafafa;
}

.accent-preview img {
  display: block;
  object-fit: contain;
}

.assign-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.assigned-users h4 {
  margin: 0 0 10px 0;
}

.user-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}

.empty-tip {
  color: #909399;
  font-style: italic;
}

.add-user {
  display: flex;
  align-items: center;
}

.accent-slider {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.slider-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  min-width: 110px;
}

.deco-param-section h4 {
  margin: 0 0 4px 0;
  font-size: 15px;
  color: #333;
}

.param-desc {
  font-size: 12px;
  color: #999;
  margin: 0 0 16px 0;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.slider-value {
  font-size: 13px;
  color: #666;
  min-width: 50px;
  text-align: right;
}
</style>
