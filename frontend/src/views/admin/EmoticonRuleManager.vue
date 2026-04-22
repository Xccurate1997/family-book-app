<template>
  <div class="emoticon-rule-manager">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>表情规则管理</span>
          <div class="header-actions">
            <el-button type="primary" @click="handleAdd">新增规则</el-button>
            <el-button type="success" @click="handleEnableAll">全部启用</el-button>
            <el-button type="warning" @click="handleDisableAll">全部禁用</el-button>
          </div>
        </div>
      </template>

      <el-table :data="rules" border stripe style="width: 100%">
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column label="表情预览" width="120">
          <template #default="{ row }">
            <div v-if="row.emojiType === 'TEXT'" class="emoji-preview">
              {{ row.emojiContent }}
            </div>
            <el-image
              v-else
              :src="`/api/emoticon-images/${row.emojiContent}`"
              fit="contain"
              style="width: 40px; height: 40px"
              preview-teleported
            />
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTransactionTypeTag(row.transactionType)">
              {{ getTransactionTypeText(row.transactionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额范围" width="150">
          <template #default="{ row }">
            {{ formatAmountRange(row.minAmount, row.maxAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80" />
        <el-table-column label="启用状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="handleToggleEnabled(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

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
        label-width="100px"
      >
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入规则名称" />
        </el-form-item>

        <el-form-item label="表情类型" prop="emojiType">
          <el-radio-group v-model="formData.emojiType">
            <el-radio label="TEXT">TEXT（Emoji字符）</el-radio>
            <el-radio label="IMAGE">IMAGE（图片上传）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="表情内容" prop="emojiContent">
          <div v-if="formData.emojiType === 'TEXT'">
            <el-input
              v-model="formData.emojiContent"
              placeholder="请输入 Emoji 字符，如 😊"
              maxlength="10"
            />
            <div v-if="formData.emojiContent" class="emoji-preview-large">
              预览：{{ formData.emojiContent }}
            </div>
          </div>
          <div v-else>
            <el-upload
              :show-file-list="false"
              :before-upload="handleBeforeUpload"
              :http-request="handleUploadImage"
              accept=".png,.jpg,.jpeg,.webp,.gif"
            >
              <el-button type="primary">选择图片</el-button>
            </el-upload>
            <div v-if="formData.emojiContent" class="image-preview">
              <el-image
                :src="`/api/emoticon-images/${formData.emojiContent}`"
                fit="contain"
                style="width: 100px; height: 100px; margin-top: 10px"
                preview-teleported
              />
            </div>
          </div>
        </el-form-item>

        <el-form-item label="交易类型" prop="transactionType">
          <el-select
            v-model="formData.transactionType"
            placeholder="请选择交易类型"
            clearable
            style="width: 100%"
          >
            <el-option label="支出" value="EXPENSE" />
            <el-option label="收入" value="INCOME" />
            <el-option label="不限" :value="null" />
          </el-select>
        </el-form-item>

        <el-form-item label="金额范围">
          <el-space>
            <el-input-number
              v-model="formData.minAmount"
              :min="0"
              :precision="2"
              placeholder="最小金额"
              style="width: 150px"
            />
            <span>至</span>
            <el-input-number
              v-model="formData.maxAmount"
              :min="0"
              :precision="2"
              placeholder="最大金额"
              style="width: 150px"
            />
          </el-space>
        </el-form-item>

        <el-form-item label="分类ID">
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
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getEmoticonRules,
  createEmoticonRule,
  updateEmoticonRule,
  deleteEmoticonRule,
  enableAllEmoticonRules,
  disableAllEmoticonRules,
  uploadEmoticonImage
} from '../../api/index.js'

const rules = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const formData = reactive({
  id: null,
  name: '',
  emojiType: 'TEXT',
  emojiContent: '',
  transactionType: null,
  minAmount: 0,
  maxAmount: null,
  categoryId: null,
  priority: 0,
  enabled: true
})

const formRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  emojiType: [{ required: true, message: '请选择表情类型', trigger: 'change' }],
  emojiContent: [{ required: true, message: '请输入表情内容', trigger: 'blur' }]
}

// 加载规则列表
const loadRules = async () => {
  try {
    const res = await getEmoticonRules()
    rules.value = res.data
  } catch (error) {
    ElMessage.error('加载规则列表失败')
  }
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    emojiType: row.emojiType,
    emojiContent: row.emojiContent,
    transactionType: row.transactionType,
    minAmount: row.minAmount,
    maxAmount: row.maxAmount,
    categoryId: row.categoryId,
    priority: row.priority,
    enabled: row.enabled
  })
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该规则吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteEmoticonRule(row.id)
    ElMessage.success('删除成功')
    await loadRules()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 切换启用状态
const handleToggleEnabled = async (row) => {
  try {
    await updateEmoticonRule(row.id, { enabled: row.enabled })
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
  } catch (error) {
    row.enabled = !row.enabled
    ElMessage.error('操作失败')
  }
}

// 全部启用
const handleEnableAll = async () => {
  try {
    await enableAllEmoticonRules()
    ElMessage.success('已全部启用')
    await loadRules()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 全部禁用
const handleDisableAll = async () => {
  try {
    await disableAllEmoticonRules()
    ElMessage.success('已全部禁用')
    await loadRules()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    const data = {
      name: formData.name,
      emojiType: formData.emojiType,
      emojiContent: formData.emojiContent,
      transactionType: formData.transactionType || null,
      minAmount: formData.minAmount,
      maxAmount: formData.maxAmount,
      categoryId: formData.categoryId || null,
      priority: formData.priority,
      enabled: formData.enabled
    }

    if (isEdit.value) {
      await updateEmoticonRule(formData.id, data)
      ElMessage.success('更新成功')
    } else {
      await createEmoticonRule(data)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    await loadRules()
  } catch (error) {
    if (error !== false) {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    }
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    id: null,
    name: '',
    emojiType: 'TEXT',
    emojiContent: '',
    transactionType: null,
    minAmount: 0,
    maxAmount: null,
    categoryId: null,
    priority: 0,
    enabled: true
  })
  formRef.value?.clearValidate()
}

// 上传前校验
const handleBeforeUpload = (file) => {
  const validTypes = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('只支持 png/jpg/webp/gif 格式的图片')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

// 上传图片
const handleUploadImage = async ({ file }) => {
  try {
    const res = await uploadEmoticonImage(file)
    formData.emojiContent = res.data.filename
    ElMessage.success('上传成功')
  } catch (error) {
    ElMessage.error('上传失败')
  }
}

// 获取交易类型标签
const getTransactionTypeTag = (type) => {
  if (type === 'EXPENSE') return 'danger'
  if (type === 'INCOME') return 'success'
  return 'info'
}

// 获取交易类型文本
const getTransactionTypeText = (type) => {
  if (type === 'EXPENSE') return '支出'
  if (type === 'INCOME') return '收入'
  return '不限'
}

// 格式化金额范围
const formatAmountRange = (min, max) => {
  if (min === 0 && max === null) return '无限制'
  if (max === null) return `≥ ${min}`
  if (min === 0) return `≤ ${max}`
  return `${min} - ${max}`
}

onMounted(() => {
  loadRules()
})
</script>

<style scoped>
.emoticon-rule-manager {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.emoji-preview {
  font-size: 24px;
  text-align: center;
}

.emoji-preview-large {
  margin-top: 10px;
  font-size: 32px;
  color: #606266;
}

.image-preview {
  margin-top: 10px;
}
</style>
