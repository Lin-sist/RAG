<!-- 反馈弹窗 — 阶段 8 -->
<template>
  <el-dialog
    v-model="visible"
    title="提交反馈"
    width="480px"
    destroy-on-close
    @close="handleClose"
  >
    <div class="feedback-form">
      <p class="feedback-question">{{ questionText }}</p>

      <!-- 评分 -->
      <div class="feedback-section">
        <label class="section-label">评分</label>
        <div class="star-row">
          <el-rate
            v-model="form.rating"
            :texts="['很差', '较差', '一般', '不错', '很好']"
            show-text
            :colors="['#F56C6C', '#E6A23C', '#E6A23C', '#409EFF', '#67C23A']"
          />
        </div>
      </div>

      <!-- 评论 -->
      <div class="feedback-section">
        <label class="section-label">评论（可选）</label>
        <el-input
          v-model="form.comment"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入你对这次回答的评价..."
        />
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!form.rating" @click="handleSubmit">
        提交反馈
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { submitFeedback } from '@/api/history'

const props = defineProps<{
  historyId: number
  questionText: string
}>()

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = defineModel<boolean>('visible', { default: false })

const submitting = ref(false)
const form = reactive({
  rating: 0,
  comment: '',
})

async function handleSubmit() {
  if (!form.rating) {
    ElMessage.warning('请选择评分')
    return
  }
  submitting.value = true
  try {
    await submitFeedback(props.historyId, {
      rating: form.rating,
      comment: form.comment || undefined,
    })
    ElMessage.success('反馈提交成功')
    visible.value = false
    emit('success')
  } catch (e: any) {
    const msg = e?.response?.data?.message || '提交失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

function handleClose() {
  form.rating = 0
  form.comment = ''
}
</script>

<style scoped>
.feedback-form {
  padding: 0 4px;
}

.feedback-question {
  background: #f5f7fa;
  padding: 10px 14px;
  border-radius: 6px;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 20px;
  border-left: 3px solid #409eff;
}

.feedback-section {
  margin-bottom: 18px;
}

.section-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}

.star-row {
  padding: 4px 0;
}
</style>
