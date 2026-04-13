<template>
  <div class="pagination-container">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="pageSizes"
      :total="total"
      :layout="layout"
      :background="background"
      :disabled="disabled"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
    
    <!-- 快捷跳转 -->
    <div v-if="showJump" class="jump-input">
      <span>共 {{ total }} 条</span>
      <el-input-number 
        v-model="jumpPage" 
        :min="1" 
        :max="totalPages" 
        size="small"
        @change="handleJump"
      />
      <el-button size="small" @click="handleJump">跳转</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  // 当前页码
  currentPage: {
    type: Number,
    default: 1
  },
  // 每页数量
  pageSize: {
    type: Number,
    default: 10
  },
  // 总条数
  total: {
    type: Number,
    default: 0
  },
  // 每页数量选项
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50, 100]
  },
  // 布局
  layout: {
    type: String,
    default: 'total, sizes, prev, pager, next, jumper'
  },
  // 是否显示背景色
  background: {
    type: Boolean,
    default: true
  },
  // 是否禁用
  disabled: {
    type: Boolean,
    default: false
  },
  // 是否显示快捷跳转
  showJump: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:currentPage', 'update:pageSize', 'change'])

const currentPage = ref(props.currentPage)
const pageSize = ref(props.pageSize)
const jumpPage = ref(props.currentPage)

// 总页数
const totalPages = computed(() => {
  return Math.ceil(props.total / props.pageSize) || 1
})

// 监听内部变化，同步到外部
watch(currentPage, (newVal) => {
  emit('update:currentPage', newVal)
  emit('change', { currentPage: newVal, pageSize: pageSize.value })
})

watch(pageSize, (newVal) => {
  emit('update:pageSize', newVal)
  currentPage.value = 1 // 改变每页数量时重置到第一页
  emit('change', { currentPage: 1, pageSize: newVal })
})

// 监听外部变化，同步到内部
watch(() => props.currentPage, (newVal) => {
  currentPage.value = newVal
  jumpPage.value = newVal
})

watch(() => props.pageSize, (newVal) => {
  pageSize.value = newVal
})

// 每页数量变化
const handleSizeChange = (size) => {
  pageSize.value = size
}

// 页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
}

// 快捷跳转
const handleJump = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    currentPage.value = jumpPage.value
  } else {
    jumpPage.value = currentPage.value
  }
}
</script>

<style scoped>
.pagination-container {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 20px;
  gap: 15px;
}

.jump-input {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #666;
}

.jump-input .el-input-number {
  width: 80px;
}
</style>