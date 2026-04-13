<template>
  <div class="upload-video">
    <el-upload
      ref="uploadRef"
      :action="uploadUrl"
      :headers="headers"
      :show-file-list="false"
      :on-success="handleSuccess"
      :on-error="handleError"
      :before-upload="beforeUpload"
      :accept="accept"
      :disabled="disabled"
      class="video-uploader"
    >
      <!-- 已有视频时显示 -->
      <div v-if="videoUrl" class="video-preview">
        <video 
          :src="videoUrl" 
          controls 
          class="uploaded-video"
        />
        <div class="video-mask">
          <el-icon class="icon-edit"><Edit /></el-icon>
          <span>更换视频</span>
        </div>
        <el-icon class="icon-delete" @click.stop="handleDelete"><Delete /></el-icon>
      </div>
      
      <!-- 无视频时显示上传按钮 -->
      <div v-else class="upload-placeholder">
        <el-icon class="upload-icon"><VideoCamera /></el-icon>
        <div class="upload-text">点击上传视频</div>
        <div class="upload-tip">{{ tip }}</div>
      </div>
    </el-upload>
    
    <!-- 上传进度 -->
    <el-progress 
      v-if="uploading" 
      :percentage="uploadProgress" 
      style="margin-top: 10px"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoCamera, Edit, Delete } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  uploadUrl: {
    type: String,
    required: true
  },
  accept: {
    type: String,
    default: 'video/*'
  },
  maxSize: {
    type: Number,
    default: 100
  },
  tip: {
    type: String,
    default: '支持 mp4 格式，不超过 100MB'
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'success', 'error'])

const uploadRef = ref(null)
const videoUrl = ref(props.modelValue)
const uploading = ref(false)
const uploadProgress = ref(0)

const headers = computed(() => {
  const token = getToken()
  return {
    'Authorization': token ? `Bearer ${token}` : ''
  }
})

const beforeUpload = (file) => {
  const isValidType = file.type.includes('video')
  if (!isValidType) {
    ElMessage.error('请上传视频文件')
    return false
  }
  
  const maxSize = props.maxSize * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
    return false
  }
  
  uploading.value = true
  uploadProgress.value = 0
  return true
}

const handleSuccess = (response) => {
  uploading.value = false
  uploadProgress.value = 100
  
  if (response.code === 200) {
    const url = response.data?.url || response.data
    videoUrl.value = url
    emit('update:modelValue', url)
    emit('success', { response, url })
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
    emit('error', response)
  }
}

const handleError = (error) => {
  uploading.value = false
  ElMessage.error('上传失败：' + error.message)
  emit('error', error)
}

const handleDelete = () => {
  videoUrl.value = ''
  emit('update:modelValue', '')
  uploadRef.value?.clearFiles()
}

watch(() => props.modelValue, (newVal) => {
  videoUrl.value = newVal
})
</script>

<style scoped>
.upload-video {
  display: inline-block;
}

.video-uploader {
  width: 100%;
}

.video-preview {
  position: relative;
  width: 300px;
  height: 170px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}

.video-preview:hover .video-mask {
  opacity: 1;
}

.uploaded-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
}

.icon-edit {
  font-size: 24px;
  margin-bottom: 5px;
}

.icon-delete {
  position: absolute;
  top: 5px;
  right: 5px;
  font-size: 18px;
  color: #fff;
  cursor: pointer;
  padding: 5px;
  background-color: rgba(255, 0, 0, 0.7);
  border-radius: 50%;
}

.upload-placeholder {
  width: 300px;
  height: 170px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #fafafa;
}

.upload-placeholder:hover {
  border-color: #409EFF;
  color: #409EFF;
}

.upload-icon {
  font-size: 48px;
  color: #8c939d;
  margin-bottom: 10px;
}

.upload-text {
  font-size: 14px;
  color: #8c939d;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}
</style>