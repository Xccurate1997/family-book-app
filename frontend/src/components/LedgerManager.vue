<template>
  <el-dialog v-model="visible" title="账本管理" :width="windowWidth < 640 ? '92%' : '420px'" :close-on-click-modal="false">
    <div class="ledger-list">
      <div v-for="l in ledgers" :key="l.id" class="ledger-item">
        <template v-if="editingId === l.id">
          <el-input v-model="editForm.icon" style="width:52px" placeholder="图标" />
          <el-input v-model="editForm.name" style="flex:1" placeholder="账本名称" />
          <el-input v-model="editForm.color" style="width:80px" placeholder="颜色" />
          <el-button type="primary" text size="small" @click="saveEdit(l.id)">保存</el-button>
          <el-button text size="small" @click="editingId = null">取消</el-button>
        </template>
        <template v-else>
          <span class="ledger-icon">{{ l.icon || '📒' }}</span>
          <span class="ledger-name">{{ l.name }}</span>
          <div class="ledger-actions">
            <el-button text size="small" :icon="Edit" @click="startEdit(l)" />
            <el-button text size="small" :icon="Delete" type="danger" @click="handleDelete(l)" />
          </div>
        </template>
      </div>
    </div>

    <el-divider />

    <!-- 新增 -->
    <div class="new-ledger">
      <el-input v-model="newForm.icon" style="width:52px" placeholder="📒" />
      <el-input v-model="newForm.name" style="flex:1" placeholder="新账本名称" />
      <el-input v-model="newForm.color" style="width:80px" placeholder="#4A90D9" />
      <el-button type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, Plus } from '@element-plus/icons-vue'
import { createLedger, updateLedger, deleteLedger } from '../api/index.js'

const props = defineProps({ modelValue: Boolean, ledgers: Array })
const emit = defineEmits(['update:modelValue', 'changed'])

const windowWidth = ref(window.innerWidth)
const onResize = () => { windowWidth.value = window.innerWidth }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const editingId = ref(null)
const editForm = ref({ name: '', icon: '', color: '' })
const newForm = ref({ name: '', icon: '', color: '' })

const startEdit = (l) => {
  editingId.value = l.id
  editForm.value = { name: l.name, icon: l.icon || '', color: l.color || '' }
}

const saveEdit = async (id) => {
  if (!editForm.value.name.trim()) return
  await updateLedger(id, editForm.value)
  editingId.value = null
  ElMessage.success('已保存')
  emit('changed')
}

const handleCreate = async () => {
  if (!newForm.value.name.trim()) {
    ElMessage.warning('请输入账本名称')
    return
  }
  await createLedger(newForm.value)
  newForm.value = { name: '', icon: '', color: '' }
  ElMessage.success('创建成功')
  emit('changed')
}

const handleDelete = async (l) => {
  try {
    await ElMessageBox.confirm(`确认删除账本「${l.name}」？`, '提示', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteLedger(l.id)
    ElMessage.success('已删除')
    emit('changed')
  } catch (e) {
    if (e?.response?.data?.message) ElMessage.error(e.response.data.message)
  }
}
</script>

<style scoped>
.ledger-list { display: flex; flex-direction: column; gap: 8px; }

.ledger-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f8f9fa;
}

.ledger-icon { font-size: 20px; width: 28px; text-align: center; }
.ledger-name { flex: 1; font-size: 14px; }
.ledger-actions { display: flex; gap: 0; }

.new-ledger {
  display: flex;
  gap: 8px;
  align-items: center;
}

@media (max-width: 640px) {
  .ledger-item {
    flex-wrap: wrap;
    gap: 6px;
  }
  .new-ledger {
    flex-wrap: wrap;
    gap: 6px;
  }
}
</style>
