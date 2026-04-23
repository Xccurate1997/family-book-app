<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑记录' : '新增记录'"
    :width="windowWidth < 640 ? '92%' : '460px'"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
      <el-form-item label="类型">
        <el-radio-group v-model="form.type" @change="onTypeChange">
          <el-radio-button value="EXPENSE">支出</el-radio-button>
          <el-radio-button value="INCOME">收入</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="金额" prop="amount">
        <el-input-number
          v-model="form.amount"
          :precision="2"
          :min="0.01"
          :step="10"
          :controls="false"
          placeholder="请输入金额"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
          <el-option
            v-for="c in filteredCategories"
            :key="c.id"
            :label="`${c.icon} ${c.name}`"
            :value="c.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="日期" prop="transactionDate">
        <el-date-picker
          v-model="form.transactionDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择日期"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="备注">
        <el-input
          v-model="form.description"
          placeholder="选填"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="saving">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { createTransaction, updateTransaction } from '../api/index.js'

const props = defineProps({
  modelValue: Boolean,
  transaction: Object,
  categories: Array,
  ledgerId: Number,
  defaultDate: String
})
const emit = defineEmits(['update:modelValue', 'saved'])

const windowWidth = ref(window.innerWidth)
const onResize = () => { windowWidth.value = window.innerWidth }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const isEdit = computed(() => !!props.transaction)

const formRef = ref()
const saving = ref(false)

const defaultForm = () => ({
  type: 'EXPENSE',
  amount: null,
  categoryId: null,
  description: '',
  transactionDate: props.defaultDate || dayjs().format('YYYY-MM-DD')
})

const form = ref(defaultForm())

const filteredCategories = computed(() =>
  (props.categories || []).filter(c => c.type === form.value.type)
)

const rules = {
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  transactionDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

// Populate form when editing
watch(() => props.transaction, (tx) => {
  if (tx) {
    form.value = {
      type: tx.type,
      amount: Number(tx.amount),
      categoryId: tx.category?.id ?? null,
      description: tx.description || '',
      transactionDate: tx.transactionDate
    }
  }
})

const onTypeChange = () => {
  form.value.categoryId = null
}

const resetForm = () => {
  form.value = defaultForm()
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const data = {
      ledgerId: props.ledgerId,
      amount: form.value.amount,
      type: form.value.type,
      categoryId: form.value.categoryId,
      description: form.value.description || null,
      transactionDate: form.value.transactionDate
    }
    if (isEdit.value) {
      await updateTransaction(props.transaction.id, data)
      ElMessage.success('已更新')
    } else {
      await createTransaction(data)
      ElMessage.success('记录成功')
    }
    visible.value = false
    emit('saved')
  } catch {
    ElMessage.error('操作失败，请重试')
  } finally {
    saving.value = false
  }
}
</script>
