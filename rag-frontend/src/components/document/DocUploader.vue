<!-- 文件上传组件 — 阶段 6 -->
<template>
  <div class="doc-uploader">
    <el-upload
      ref="uploadRef"
      drag
      :auto-upload="false"
      :file-list="fileList"
      :accept="acceptTypes"
      :on-change="handleFileChange"
      :on-remove="handleFileRemove"
      :limit="5"
      :on-exceed="handleExceed"
      multiple
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">
        将文件拖到此处，或 <em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持 PDF、Markdown、Word、TXT、代码文件，单次最多 5 个文件
        </div>
      </template>
    </el-upload>

    <div class="upload-actions" v-if="fileList.length > 0">
      <el-button type="primary" :loading="uploading" @click="submitUpload">
        <el-icon><Upload /></el-icon>
        开始上传（{{ fileList.length }} 个文件）
      </el-button>
      <el-button @click="clearFiles" :disabled="uploading">清空列表</el-button>
    </div>

    <!-- 上传结果提示 -->
    <div v-if="uploadResults.length > 0" class="upload-results">
      <el-alert
        v-for="(result, index) in uploadResults"
        :key="index"
        :title="result.message"
        :type="result.success ? 'success' : 'error'"
        :closable="true"
        show-icon
        class="result-alert"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { UploadFilled, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadInstance } from 'element-plus'
import { uploadDocument } from '@/api/knowledgeBase'
import type { DocumentUploadResponse } from '@/types/document'

const props = defineProps<{
  kbId: number
}>()

const emit = defineEmits<{
  /** 单个文件上传成功后触发，传递 taskId 用于轮询 */
  (e: 'uploaded', data: DocumentUploadResponse): void
  /** 全部文件上传完成后触发 */
  (e: 'allDone'): void
}>()

const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadFile[]>([])
const uploading = ref(false)

// 支持的文件类型
const acceptTypes = '.pdf,.md,.markdown,.doc,.docx,.txt,.java,.py,.js,.ts,.go,.c,.cpp,.h,.vue,.json,.yml,.yaml,.xml,.html,.css'

interface UploadResult {
  success: boolean
  message: string
}
const uploadResults = ref<UploadResult[]>([])

function handleFileChange(_file: UploadFile, newFileList: UploadFile[]) {
  fileList.value = newFileList
}

function handleFileRemove(_file: UploadFile, newFileList: UploadFile[]) {
  fileList.value = newFileList
}

function handleExceed() {
  ElMessage.warning('单次最多上传 5 个文件')
}

function clearFiles() {
  fileList.value = []
  uploadResults.value = []
  uploadRef.value?.clearFiles()
}

/** 逐个上传文件，每个成功后发射 uploaded 事件 */
async function submitUpload() {
  if (fileList.value.length === 0) return

  uploading.value = true
  uploadResults.value = []

  for (const item of fileList.value) {
    const rawFile = item.raw
    if (!rawFile) continue

    try {
      const res = await uploadDocument(props.kbId, rawFile, rawFile.name)
      const data = res.data.data
      uploadResults.value.push({ success: true, message: `「${rawFile.name}」上传成功，任务已提交` })
      emit('uploaded', data)
    } catch (err: any) {
      const msg = err?.message || '上传失败'
      uploadResults.value.push({ success: false, message: `「${rawFile.name}」${msg}` })
    }
  }

  uploading.value = false
  fileList.value = []
  uploadRef.value?.clearFiles()
  emit('allDone')
}
</script>

<style scoped>
.doc-uploader {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.upload-actions {
  display: flex;
  gap: 10px;
}
.upload-results {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.result-alert {
  margin: 0;
}
</style>
