<template>
  <el-header class="app-header">
    <div class="header-left">
      <el-icon class="collapse-btn" @click="emit('toggle-collapse')">
        <Fold v-if="!collapsed" />
        <Expand v-else />
      </el-icon>
      <div class="header-title-area">
        <h3 class="header-title">{{ title }}</h3>
      </div>
    </div>
    <div class="header-right">
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="user-dropdown">
          <el-avatar :size="32" class="user-avatar">
            {{ username.charAt(0).toUpperCase() }}
          </el-avatar>
          <span class="username">{{ username }}</span>
          <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              <el-icon><User /></el-icon>
              {{ username }}
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { Fold, Expand, ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'

defineProps<{
  collapsed: boolean
  title: string
  username: string
}>()

const emit = defineEmits<{
  'toggle-collapse': []
  'logout': []
}>()

function handleCommand(command: string) {
  if (command === 'logout') {
    emit('logout')
  }
}
</script>

<style scoped>
.app-header {
  background: var(--rag-bg-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--rag-border);
  padding: 0 var(--rag-space-6);
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--rag-space-4);
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: var(--rag-text-secondary);
  transition: var(--rag-transition);
  padding: var(--rag-space-1);
  border-radius: var(--rag-radius-sm);
}

.collapse-btn:hover {
  color: var(--rag-primary);
  background: var(--rag-bg-hover);
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--rag-text-primary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--rag-space-4);
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: var(--rag-space-3);
  cursor: pointer;
  color: var(--rag-text-regular);
  outline: none;
  padding: var(--rag-space-2) var(--rag-space-3);
  border-radius: var(--rag-radius-sm);
  transition: var(--rag-transition);
}

.user-dropdown:hover {
  background: var(--rag-bg-hover);
  color: var(--rag-primary);
}

.user-avatar {
  background: var(--rag-gradient);
  color: #fff;
  font-weight: 600;
}

.username {
  font-size: var(--rag-font-body);
  font-weight: 500;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-arrow {
  font-size: 12px;
  color: var(--rag-text-placeholder);
}
</style>
