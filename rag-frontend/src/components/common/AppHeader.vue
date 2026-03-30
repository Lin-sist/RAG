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
    <!-- 用户信息已移至侧边栏底部头像菜单 -->
  </el-header>
</template>

<script setup lang="ts">
import { Fold, Expand } from '@element-plus/icons-vue'

defineProps<{
  collapsed: boolean
  title: string
}>()

const emit = defineEmits<{
  'toggle-collapse': []
}>()
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

</style>
