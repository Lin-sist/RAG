<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑知识库' : '创建知识库'"
    width="500px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
    @closed="resetForm"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
      label-position="top"
    >
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入知识库名称" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="请输入知识库描述（选填）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="可见性">
        <el-switch
          v-model="form.isPublic"
          active-text="公开"
          inactive-text="私有"
          inline-prompt
        />
        <span class="visibility-hint">
          {{ form.isPublic ? '所有用户可见' : '仅自己可见' }}
        </span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEdit ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase'
import type { KnowledgeBaseDTO } from '@/types/knowledgeBase'

const props = defineProps<{
  visible: boolean
  editData?: KnowledgeBaseDTO | null  // 传入则为编辑模式
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  'success': []
}>()

const kbStore = useKnowledgeBaseStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const isEdit = ref(false)
const editId = ref<number | null>(null)

const form = reactive({
  name: '',
  description: '',
  isPublic: false,
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度 2~50 个字符', trigger: 'blur' },
  ],
}

// 监听 editData，回填表单
watch(() => props.editData, (val) => {
  if (val) {
    isEdit.value = true
    editId.value = val.id
    form.name = val.name
    form.description = val.description || ''
    form.isPublic = val.isPublic
  } else {
    isEdit.value = false
    editId.value = null
  }
}, { immediate: true })

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value && editId.value !== null) {
      await kbStore.update(editId.value, {
        name: form.name,
        description: form.description || undefined,
        isPublic: form.isPublic,
      })
    } else {
      await kbStore.create({
        name: form.name,
        description: form.description || undefined,
        isPublic: form.isPublic,
      })
    }
    emit('update:visible', false)
    emit('success')
  } catch (e: any) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  formRef.value?.resetFields()
  form.name = ''
  form.description = ''
  form.isPublic = false
  isEdit.value = false
  editId.value = null
}
</script>

<style scoped>
.visibility-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
