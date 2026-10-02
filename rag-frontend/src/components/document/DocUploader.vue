<!-- 文件上传组件 — 阶段 6 -->
<template>
  <div class="doc-uploader">
    <el-upload
      ref="uploadRef"
      drag
      :auto-upload="false"
      :disabled="uploading"
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
        将文件拖到此处，或 <em>选择文件</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持 PDF、Markdown、Word(.docx)、TXT、代码文件，单次最多 5 个文件。已接受不等于索引完成。
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
        :type="result.state === 'accepted' ? 'success' : result.state === 'failed' ? 'error' : 'warning'"
        :closable="true"
        show-icon
        class="result-alert"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { UploadFilled, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadInstance } from 'element-plus'
import { uploadDocument } from '@/api/knowledgeBase'
import type { DocumentUploadResponse } from '@/types/document'
import { submitUploadBatch, type UploadOutcome } from '@/utils/uploadQueue'

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
let disposed = false
onUnmounted(() => { disposed = true })

// 支持的文件类型
const acceptTypes = '.pdf,.md,.markdown,.docx,.txt,.java,.py,.js,.ts,.go,.c,.cpp,.h,.vue,.json,.yml,.yaml,.xml,.html,.css'

const uploadResults = ref<UploadOutcome[]>([])

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
  if (uploading.value || fileList.value.length === 0) return

  uploading.value = true
  uploadResults.value = []

  const kbId = props.kbId
  const files = fileList.value.flatMap(item => item.raw ? [item.raw] : [])
  try {
    const remaining = await submitUploadBatch(files, file => uploadDocument(kbId, file, file.name), result => {
      uploadResults.value.push(result)
      if (result.data) emit('uploaded', result.data)
    }, () => !disposed)
    if (disposed) return
    fileList.value = fileList.value.filter(item => item.raw && remaining.includes(item.raw))
    emit('allDone')
  } finally { if (!disposed) uploading.value = false }
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
  flex-wrap: wrap;
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
