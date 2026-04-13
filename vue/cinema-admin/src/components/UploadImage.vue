<template>
  <div class="upload-image">
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
      class="image-uploader"
    >
      <!-- 已有图片时显示 -->
      <div v-if="imageUrl" class="image-preview">
        <el-image 
          :src="imageUrl" 
          fit="cover" 
          class="uploaded-image"
          :preview-src-list="[imageUrl]"
        />
        <div class="image-mask">
          <el-icon class="icon-edit"><Edit /></el-icon>
          <span>更换图片</span>
        </div>
        <el-icon class="icon-delete" @click.stop="handleDelete"><Delete /></el-icon>
      </div>
      
      <!-- 无图片时显示上传按钮 -->
      <div v-else class="upload-placeholder">
        <el-icon class="upload-icon"><Plus /></el-icon>
        <div class="upload-text">点击上传</div>
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
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'

const props = defineProps({
  // 已上传的图片 URL
  modelValue: {
    type: String,
    default: ''
  },
  // 上传接口地址
  uploadUrl: {
    type: String,
    required: true
  },
  // 接受的文件类型
  accept: {
    type: String,
    default: 'image/*'
  },
  // 最大文件大小（MB）
  maxSize: {
    type: Number,
    default: 5
  },
  // 提示文字
  tip: {
    type: String,
    default: '支持 jpg/png 格式，不超过 5MB'
  },
  // 是否禁用
  disabled: {
    type: Boolean,
    default: false
  },
  // 上传成功后是否自动返回 URL（有些接口返回的是对象）
  responseUrlKey: {
    type: String,
    default: 'url'
  }
})

const emit = defineEmits(['update:modelValue', 'success', 'error'])

const uploadRef = ref(null)
const imageUrl = ref(props.modelValue)
const uploading = ref(false)
const uploadProgress = ref(0)

// 请求头（携带 Token）
const headers = computed(() => {
  const token = getToken()
  return {
    'Authorization': token ? `Bearer ${token}` : ''
  }
})

// 上传前校验
const beforeUpload = (file) => {
  // 检查文件类型
  const isValidType = props.accept.includes(file.type) || 
                      props.accept.includes('*')
  if (!isValidType) {
    ElMessage.error('文件格式不正确')
    return false
  }
  
  // 检查文件大小
  const maxSize = props.maxSize * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
    return false
  }
  
  uploading.value = true
  uploadProgress.value = 0
  return true
}

// 上传成功
const handleSuccess = (response, uploadFile) => {
  uploading.value = false
  uploadProgress.value = 100
  
  if (response.code === 200) {
    // 获取图片 URL（根据接口返回结构调整）
    const url = response.data?.url || 
                response.data?.[props.responseUrlKey] || 
                response.data
    
    imageUrl.value = url
    emit('update:modelValue', url)
    emit('success', { response, url })
    
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
    emit('error', response)
  }
}

// 上传失败
const handleError = (error) => {
  uploading.value = false
  ElMessage.error('上传失败：' + error.message)
  emit('error', error)
}

// 删除图片
const handleDelete = () => {
  imageUrl.value = ''
  emit('update:modelValue', '')
  uploadRef.value?.clearFiles()
}

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  imageUrl.value = newVal
})
</script>

<style scoped>
.upload-image {
  display: inline-block;
}

.image-uploader {
  width: 100%;
}

.image-preview {
  position: relative;
  width: 150px;
  height: 150px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  transition: all 0.3s;
}

.image-preview:hover .image-mask {
  opacity: 1;
}

.uploaded-image {
  width: 100%;
  height: 100%;
}

.image-mask {
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
  width: 150px;
  height: 150px;
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