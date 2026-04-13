<template>
  <div class="actor-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true">
        <el-form-item label="演员名称">
          <el-input 
          v-model="searchName" 
            placeholder="请输入演员名称" 
            clearable 
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button type="success" @click="handleAdd">+ 新增演员</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card">
      <el-table :data="actorList" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="头像" width="100" align="center">
          <template #default="{ row }">
            <el-image 
            v-if="row.avatarUrl" 
              :src="row.avatarUrl" 
              :preview-src-list="[row.avatarUrl]"
              style="width: 60px; height: 60px" 
              fit="cover" 
            />
            <span v-else>无头像</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="演员名称" min-width="150" />
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchActorList"
          @current-change="fetchActorList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
    v-model="dialogVisible"
      :title="isEdit ? '编辑演员' : '新增演员'"
      width="500px"
      destroy-on-close
      @closed="handleDialogClose"
    >
      <el-form ref="actorFormRef" :model="actorForm" :rules="rules" label-width="100px">
        <el-form-item label="演员名称" prop="name">
          <el-input v-model="actorForm.name" placeholder="请输入演员名称" />
        </el-form-item>
        
        <el-form-item label="头像" prop="avatarFile">
          <el-upload
          ref="avatarUploadRef"
            action="#"
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :file-list="avatarFileList"
            :on-change="handleAvatarChange"
            :on-remove="handleAvatarRemove"
          >
            <el-button>选择头像</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 jpg/png 格式，不超过 5MB</div>
            </template>
          </el-upload>
          <!-- 预览已选头像 -->
          <div v-if="actorForm.avatarFile" class="avatar-preview">
            <el-image 
              :src="avatarPreviewUrl" 
              style="width: 60px; height: 60px; margin-top: 8px; border-radius: 4px" 
              fit="cover"
            />
          </div>
          <!-- 编辑时显示旧头像 -->
          <div v-if="!actorForm.avatarFile && actorForm.oldAvatarUrl" class="avatar-preview">
            <el-image 
              :src="actorForm.oldAvatarUrl" 
              style="width: 60px; height: 60px; margin-top: 8px; border-radius: 4px" 
              fit="cover"
            />
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addActor, updateActor, deleteActor, getActorPage, getActorPageFiltered } from '@/api/actor'


const actorList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const actorFormRef = ref(null)
const avatarUploadRef = ref(null)

// 分页相关
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 文件上传相关
const avatarFileList = ref([])
const avatarPreviewUrl = ref('')

const actorForm = reactive({
  id: null,
  name: '',
  avatarFile: null,
  oldAvatarUrl: ''
})

const rules = {
  name: [
    { required: true, message: '请输入演员名称', trigger: 'blur' }
  ]
}

// 搜索用独立变量
const searchName= ref('')

// 获取演员列表（分页）
// 获取演员列表（分页）
// 获取演员列表（分页）
const fetchActorList = async () => {
  loading.value = true
  try {
    let res
    
    // 有搜索条件时调用过滤接口
    if (searchName.value) {
     res = await getActorPageFiltered({
      pageNum: currentPage.value,
       pageSize: pageSize.value,
        name: searchName.value
      })
    } else {
     res = await getActorPage({
      pageNum: currentPage.value,
       pageSize: pageSize.value
      })
    }
    
    if (res.data?.list) {
      actorList.value = res.data.list
      total.value = res.data.total
    } else if (res.data?.records) {
      actorList.value = res.data.records
      total.value = res.data.total
    } else if (Array.isArray(res.data)) {
      actorList.value = res.data
      total.value = res.data.length
    } else {
      actorList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取演员列表失败:', error)
    ElMessage.error('获取演员列表失败')
    actorList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}


// 搜索按钮点击
const handleSearch = () => {
  currentPage.value = 1
  fetchActorList()
}

// 新增演员
const handleAdd = () => {
  isEdit.value = false
  Object.assign(actorForm, { 
    id: null, 
    name: '', 
    avatarFile: null, 
    oldAvatarUrl: '' 
  })
  avatarFileList.value = []
  avatarPreviewUrl.value = ''
  avatarUploadRef.value?.clearFiles()
  dialogVisible.value = true
}

// 编辑演员
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(actorForm, {
    id: row.id,
    name: row.name,
    avatarFile: null,
    oldAvatarUrl: row.avatarUrl || ''
  })
  
  if (row.avatarUrl) {
    avatarFileList.value = [{
      name: 'avatar.jpg',
    url: row.avatarUrl
    }]
    avatarPreviewUrl.value = row.avatarUrl
  } else {
    avatarFileList.value = []
    avatarPreviewUrl.value = ''
  }
  
  dialogVisible.value = true
}

// 删除演员
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除 "${row.name}" 吗？`, '警告', {
  type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await deleteActor(row.id)
      ElMessage.success('删除成功')
    fetchActorList()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

// 头像文件变化
const handleAvatarChange = (file) => {
  // 验证文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('头像大小不能超过 5MB')
  return false
  }
  
  // 验证文件类型
  const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/jpg']
  if (!validTypes.includes(file.raw?.type?.toLowerCase())) {
    ElMessage.error('只支持 jpg/png/gif 格式')
  return false
  }
  
  // 保存文件对象
  actorForm.avatarFile = file.raw
  
  // 手动更新文件列表
  avatarFileList.value = [file]
  
  // 生成预览 URL
  if (avatarPreviewUrl.value && avatarPreviewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
  }
  avatarPreviewUrl.value = URL.createObjectURL(file.raw)
  
  return false
}

// 处理文件移除
const handleAvatarRemove = () => {
  if (avatarPreviewUrl.value?.startsWith('blob:')) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
  }
  
  actorForm.avatarFile = null
  avatarFileList.value = []
  avatarPreviewUrl.value = ''
}

// 提交表单
const handleSubmit = async () => {
  if (!actorFormRef.value) return
  
  try {
    await actorFormRef.value.validate()
    
    submitLoading.value = true
    
    // 构建 FormData
    const formData = new FormData()
    
    // 添加演员 JSON 数据
    const actorJson= JSON.stringify({
      id: actorForm.id,
      name: actorForm.name
    })
    formData.append('actor', actorJson)
    
    // 添加头像文件
    if (actorForm.avatarFile) {
      formData.append('avatarFile', actorForm.avatarFile)
    }
    
    // 添加旧头像 URL（编辑时需要）
    if (actorForm.oldAvatarUrl) {
      formData.append('oldAvatarUrl', actorForm.oldAvatarUrl)
    }
    
    if (isEdit.value) {
      await updateActor(formData)
      ElMessage.success('更新成功')
    } else {
      await addActor(formData)
      ElMessage.success('添加成功')
    }
    
    dialogVisible.value = false
  fetchActorList()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 关闭对话框时清理资源
const handleDialogClose = () => {
  if (avatarUploadRef.value) {
    avatarUploadRef.value.clearFiles()
  }
  avatarFileList.value = []
  if (avatarPreviewUrl.value?.startsWith('blob:')) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
  }
  avatarPreviewUrl.value = ''
  actorFormRef.value?.resetFields()
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
  day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 页面挂载时加载数据
onMounted(() => {
  fetchActorList()
})

// 页面卸载时清理资源
onBeforeUnmount(() => {
  if (avatarPreviewUrl.value?.startsWith('blob:')) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
  }
})
</script>

<style scoped>
.actor-manage {
  padding: 20px;
}
.search-card, .table-card {
  margin-bottom: 20px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 头像预览样式 */
.avatar-preview {
  margin-top: 8px;
}
.avatar-preview .el-image {
  border-radius: 4px;
  border: 1px solid #ebeef5;
  transition: opacity 0.3s;
}
.avatar-preview .el-image:hover {
  opacity: 0.9;
}
</style>