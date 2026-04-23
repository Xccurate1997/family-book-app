<template>
  <div class="cat-manager">
    <div v-for="(group, type) in grouped" :key="type" class="cat-group">
      <div class="group-title">{{ type === 'EXPENSE' ? '💸 支出分类' : '💰 收入分类' }}</div>

      <div v-for="c in group" :key="c.id" class="cat-item">
        <template v-if="editingId === c.id">
          <el-input v-model="editForm.icon" style="width:52px" placeholder="图标" />
          <el-input v-model="editForm.name" style="flex:1" />
          <el-button type="primary" text size="small" @click="saveEdit(c.id, type)">保存</el-button>
          <el-button text size="small" @click="editingId = null">取消</el-button>
        </template>
        <template v-else>
          <span class="cat-icon">{{ c.icon }}</span>
          <span class="cat-name">{{ c.name }}</span>
          <div class="cat-actions">
            <el-button text size="small" :icon="Edit" @click="startEdit(c)" />
            <el-button text size="small" :icon="Delete" type="danger" @click="handleDelete(c)" />
          </div>
        </template>
      </div>

      <!-- 新增该类型分类 -->
      <div class="cat-add">
        <el-input v-model="newForms[type].icon" style="width:52px" placeholder="图标" />
        <el-input v-model="newForms[type].name" style="flex:1" placeholder="新分类名称" />
        <el-button :icon="Plus" size="small" @click="handleCreate(type)">添加</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, Plus } from '@element-plus/icons-vue'
import { createCategory, updateCategory, deleteCategory } from '../api/index.js'

const props = defineProps({ categories: Array })
const emit = defineEmits(['changed'])

const grouped = computed(() => {
  const map = { EXPENSE: [], INCOME: [] }
  for (const c of (props.categories || [])) {
    map[c.type]?.push(c)
  }
  return map
})

const editingId = ref(null)
const editForm = ref({ name: '', icon: '' })
const newForms = ref({ EXPENSE: { name: '', icon: '' }, INCOME: { name: '', icon: '' } })

const startEdit = (c) => {
  editingId.value = c.id
  editForm.value = { name: c.name, icon: c.icon || '' }
}

const saveEdit = async (id, type) => {
  if (!editForm.value.name.trim()) return
  await updateCategory(id, { name: editForm.value.name, type, icon: editForm.value.icon })
  editingId.value = null
  ElMessage.success('已保存')
  emit('changed')
}

const handleCreate = async (type) => {
  const f = newForms.value[type]
  if (!f.name.trim()) { ElMessage.warning('请输入分类名称'); return }
  await createCategory({ name: f.name, type, icon: f.icon })
  newForms.value[type] = { name: '', icon: '' }
  ElMessage.success('创建成功')
  emit('changed')
}

const handleDelete = async (c) => {
  try {
    await ElMessageBox.confirm(`确认删除分类「${c.name}」？`, '提示', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteCategory(c.id)
    ElMessage.success('已删除')
    emit('changed')
  } catch (e) {
    if (e?.response?.data?.message) ElMessage.error(e.response.data.message)
  }
}
</script>

<style scoped>
.cat-manager { display: flex; flex-direction: column; gap: 20px; }

.cat-group {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
}

.group-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #303133;
}

.cat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f5;
}
.cat-item:last-of-type { border-bottom: none; }

.cat-icon { font-size: 18px; width: 28px; text-align: center; }
.cat-name { flex: 1; font-size: 14px; }
.cat-actions { display: flex; gap: 0; opacity: 0; transition: opacity .2s; }
.cat-item:hover .cat-actions { opacity: 1; }

/* 触摸设备始终显示操作按钮 */
@media (hover: none) {
  .cat-actions { opacity: 1; }
}

.cat-add {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e4e7ed;
}
</style>
