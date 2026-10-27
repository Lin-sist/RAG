<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑知识库' : '创建知识库'"
    width="min(500px, calc(100vw - 32px))"
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    @update:model-value="emit('update:visible', $event)"
    @closed="resetForm"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
      label-position="top"
      :disabled="submitting"
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
      <el-form-item label="可见范围">
        <el-switch
          v-model="form.isPublic"
          active-text="组织内公开"
          inactive-text="非公开"
          inline-prompt
        />
        <span class="visibility-hint">
          {{ form.isPublic ? '本工作空间内的用户可以读取，其他工作空间不可见。' : '创建者及已获授权的本工作空间用户可以访问。' }}
        </span>
      </el-form-item>
    </el-form>
    <el-alert v-if="submitError" :title="submitError" type="error" :closable="false" show-icon />
    <template #footer>
      <el-button :disabled="submitting" @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEdit ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onUnmounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { normalizeError } from '@/api/errors'
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
const submitError = ref('')
let disposed = false
onUnmounted(() => { disposed = true })

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

// 每次打开都回填，即使 editData 仍为同一对象。
watch(() => [props.visible, props.editData] as const, ([visible, val]) => {
  if (!visible) return
  resetForm()
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
  if (submitting.value) return
  submitting.value = true
  submitError.value = ''
  const target = editId.value
  try {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid || disposed) return
    if (target !== null) {
      await kbStore.update(target, {
        name: form.name,
        description: form.description,
        isPublic: form.isPublic,
      })
    } else {
      await kbStore.create({
        name: form.name,
        description: form.description || undefined,
        isPublic: form.isPublic,
      })
    }
    if (disposed) return
    emit('update:visible', false)
    emit('success')
  } catch (error) {
    if (!disposed) submitError.value = normalizeError(error).message
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  formRef.value?.clearValidate()
  submitError.value = ''
  form.name = ''
  form.description = ''
  form.isPublic = false
  isEdit.value = false
  editId.value = null
}
</script>

<style scoped>
.visibility-hint {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: var(--rag-text-secondary);
}
</style>
