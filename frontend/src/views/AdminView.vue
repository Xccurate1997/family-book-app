<template>
  <div class="admin-page">
    <!-- Header -->
    <div class="admin-header">
      <h1>📒 家庭记账本 - 管理后台</h1>
      <div class="admin-user-info">
        <span>{{ currentUser?.nickname || '管理员' }}</span>
        <el-button type="danger" size="small" @click="handleLogout">退出登录</el-button>
      </div>
    </div>

    <!-- Body: 左侧菜单 + 右侧内容 -->
    <div class="admin-body">
      <div class="admin-sidebar">
        <el-menu
          :default-active="activeMenu"
          @select="handleMenuSelect"
          class="sidebar-menu"
        >
          <el-menu-item index="emoticon">
            <span>😊 表情规则</span>
          </el-menu-item>
          <el-menu-item index="effect">
            <span>🎆 彩蛋规则</span>
          </el-menu-item>
          <el-menu-item index="theme">
            <span>🎨 主题管理</span>
          </el-menu-item>
        </el-menu>
      </div>

      <div class="admin-main">
        <EmoticonRuleManager v-if="activeMenu === 'emoticon'" />
        <EffectRuleManager v-if="activeMenu === 'effect'" />
        <ThemeManager v-if="activeMenu === 'theme'" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '../composables/useAuth.js'
import EmoticonRuleManager from './admin/EmoticonRuleManager.vue'
import EffectRuleManager from './admin/EffectRuleManager.vue'
import ThemeManager from './admin/ThemeManager.vue'

const router = useRouter()
const { currentUser, clearAuth } = useAuth()
const activeMenu = ref('emoticon')

function handleMenuSelect(index) {
  activeMenu.value = index
}

function handleLogout() {
  clearAuth()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.admin-header {
  background: white;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
  z-index: 10;
}

.admin-header h1 {
  font-size: 18px;
  margin: 0;
  color: #333;
}

.admin-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #666;
}

.admin-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.admin-sidebar {
  width: 180px;
  background: white;
  border-right: 1px solid #e6e6e6;
  flex-shrink: 0;
}

.sidebar-menu {
  border-right: none;
  padding-top: 8px;
}

.admin-main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  height: calc(100vh - 56px);
}

@media (max-width: 640px) {
  .admin-header {
    padding: 0 12px;
  }
  .admin-header h1 {
    font-size: 14px;
  }
  .admin-body {
    flex-direction: column;
  }
  .admin-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #e6e6e6;
  }
  .sidebar-menu {
    display: flex;
    padding-top: 0;
  }
  .sidebar-menu .el-menu-item {
    flex: 1;
    text-align: center;
    padding: 0 8px !important;
    font-size: 13px;
  }
  .admin-main {
    padding: 12px;
    height: auto;
    min-height: 0;
    flex: 1;
    overflow-y: auto;
  }
}
</style>
