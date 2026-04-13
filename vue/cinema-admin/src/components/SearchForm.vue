<template>
  <el-card class="search-form-card">
    <el-form :inline="true" :model="formData" class="search-form">
      <slot name="items" :formData="formData" />
      
      <el-form-item>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
        <slot name="extra" />
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  // 初始表单数据
  initialData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['search', 'reset'])

const formData = reactive({ ...props.initialData })

const handleSearch = () => {
  emit('search', { ...formData })
}

const handleReset = () => {
  Object.keys(formData).forEach(key => {
    formData[key] = props.initialData[key] || ''
  })
  emit('reset', { ...formData })
}

// 暴露方法给父组件
defineExpose({
  formData,
  reset: handleReset
})
</script>

<style scoped>
.search-form-card {
  margin-bottom: 20px;
}

.search-form {
  padding: 10px 0;
}
</style>