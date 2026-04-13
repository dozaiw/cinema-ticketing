<template>
  <div class="advertisement-container">
    <!-- 顶部操作栏 -->
    <el-card class="header-card">
      <div class="header-content">
        <h3>广告管理</h3>
        <el-button type="primary" @click="openDialog('add')">
          <el-icon><Plus /></el-icon>新增广告
        </el-button>
      </div>
    </el-card>

    <!-- 搜索过滤 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" size="small">
        <el-form-item label="标题">
          <el-input 
            v-model="searchForm.title" 
            placeholder="请输入广告标题" 
            clearable 
            @keyup.enter="fetchList"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table 
        :data="tableData" 
        v-loading="loading" 
        border 
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="广告标题" min-width="150" show-overflow-tooltip />
        
        <el-table-column label="广告图片" width="100" align="center">
          <template #default="{ row }">
            <el-image 
              v-if="row.imageUrl" 
              :src="row.imageUrl" 
              :preview-src-list="[row.imageUrl]"
              fit="cover"
              class="ad-thumb"
            />
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="linkUrl" label="跳转链接" min-width="120" show-overflow-tooltip />
        <el-table-column label="关联电影" width="100" align="center">
          <template #default="{ row }">
            {{ row.movieId ? `ID:${row.movieId}` : '-' }}
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDialog('edit', row)">编辑</el-button>
            <el-button 
              link 
              :type="row.status === 1 ? 'warning' : 'success'" 
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogType === 'add' ? '新增广告' : '编辑广告'"
      width="550px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="广告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入广告标题" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="广告图片">
          <el-upload
            v-model:file-list="fileList"
            action="#"
            :http-request="handleUpload"
            :limit="1"
            :before-upload="beforeUpload"
            accept="image/*"
            list-type="picture-card"
            :disabled="dialogType === 'edit' && fileList.length > 0 && !isImageChanged"
          >
            <el-icon><Plus /></el-icon>
            <template #file="{ file }">
              <img class="el-upload-list__item-thumbnail" :src="file.url" alt="" />
              <span class="el-upload-list__item-actions">
                <span class="el-upload-list__item-preview" @click="previewImage(file)">
                  <el-icon><ZoomIn /></el-icon>
                </span>
                <span 
                  class="el-upload-list__item-delete" 
                  @click="() => { removeImage(file); isImageChanged = true }"
                >
                  <el-icon><Delete /></el-icon>
                </span>
              </span>
            </template>
          </el-upload>
          <div class="upload-tip">支持 JPG/PNG，最大 5MB</div>
        </el-form-item>

        <el-form-item label="跳转链接" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="可选，点击后跳转路径" />
        </el-form-item>

        <el-form-item label="关联电影" prop="movieId">
          <el-select v-model="form.movieId" placeholder="可选" clearable style="width: 100%">
            <el-option 
              v-for="movie in movieOptions" 
              :key="movie.id" 
              :label="movie.title" 
              :value="movie.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="排序值" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width: 100%" />
          <span class="form-tip">越大越靠前，默认 0</span>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="图片预览" width="500px" :show-close="false">
      <img :src="previewUrl" style="width: 100%" />
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, ZoomIn } from '@element-plus/icons-vue'
// 导入封装好的API，避免重复代码
import {
  getAdvertisementPage,
  addAdvertisement,
  updateAdvertisement,
  deleteAdvertisement,
  toggleAdvertisementStatus
} from '@/api/advertisement'
import { getHotMovies } from '@/api/movie' // 新增导入

// ============ 状态定义 ============
const searchForm = reactive({ 
  title: '',
  status: undefined // 新增状态筛选
})
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogType = ref('add')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null, 
  title: '', 
  imageUrl: '', // 仅用于回显，不参与提交
  linkUrl: '',
  movieId: null, 
  status: 1, 
  sortOrder: 0, 
  isDeleted: 0
})
const fileList = ref([])
const previewVisible = ref(false)
const previewUrl = ref('')
const movieOptions = ref([])
const isImageChanged = ref(false) // 标记图片是否修改

// 表单校验规则
const rules = {
  title: [
    { required: true, message: '请输入广告标题', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2-50 个字符', trigger: 'blur' }
  ],
  sortOrder: [
    { required: true, message: '请输入排序值', trigger: 'blur' },
    { type: 'number', min: 0, max: 999, message: '排序值必须在 0-999 之间', trigger: 'blur' }
  ]
}

// ============ 辅助方法 ============
/** 
 * 获取热映电影列表（用于关联选择）
 * 文档接口 15-16: GET /movie/public/hot/list
 */
const fetchHotMovies = async () => {
  try {
    const res = await getHotMovies({ pageNum: 1, pageSize: 100 })
    if (res && res.code === 200) {
      // 根据后端返回结构调整：常见结构为 { code, data: { records: [...] } }
      const records = res.data?.records || res.data || []
      movieOptions.value = records.map(item => ({
        label: item.name || item.title || item.movieName || '',
        value: item.id
      }))
    }
  } catch (err) {
    console.error('获取电影列表失败', err)
    ElMessage.warning('获取电影列表失败，关联电影功能暂时不可用')
  }
}

// ============ 核心方法 ============

// 页面初始化时获取列表和电影选项
onMounted(() => {
  fetchList()
  fetchHotMovies()
})


/**
 * 获取广告列表
 */
const fetchList = async () => {
  loading.value = true
  try {
    // 使用封装好的API
    const res = await getAdvertisementPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      title: searchForm.title || undefined,
      status: searchForm.status // 新增状态筛选
    })
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      pagination.total = res.data?.total || 0
    } else {
      ElMessage.error(res.msg || '获取列表失败')
    }
  } catch (err) {
    console.error('fetchList error:', err)
    ElMessage.error('获取列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 重置搜索条件
 */
const resetSearch = () => {
  searchForm.title = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  fetchList()
}

/**
 * 打开新增/编辑对话框
 */
const openDialog = (type, row = null) => {
  dialogType.value = type
  dialogVisible.value = true
  isImageChanged.value = false // 重置图片修改标记
  
  if (type === 'edit' && row) {
    // 编辑时赋值
    Object.assign(form, {
      id: row.id, 
      title: row.title, 
      imageUrl: row.imageUrl, // 保存原图URL
      linkUrl: row.linkUrl, 
      movieId: row.movieId,
      status: row.status, 
      sortOrder: row.sortOrder || 0, 
      isDeleted: row.isDeleted || 0
    })
    
    // 图片回显
    if (row.imageUrl) {
      fileList.value = [{ 
        name: `ad-${row.id}`, 
        url: row.imageUrl, 
        uid: `edit-${row.id}`,
        isPreview: true // 标记为预览图片，不是新上传的
      }]
    } else {
      fileList.value = []
    }
  } else {
    // 新增时重置表单
    resetForm()
  }
}

/**
 * 重置表单
 */
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  fileList.value = []
  isImageChanged.value = false
  Object.assign(form, {
    id: null, 
    title: '', 
    imageUrl: '', 
    linkUrl: '',
    movieId: null, 
    status: 1, 
    sortOrder: 0, 
    isDeleted: 0
  })
}

/**
 * 处理图片上传（本地预览）
 */
const handleUpload = async (options) => {
  const { file, onSuccess, onError } = options
  try {
    // 直接保存文件对象，不转base64
    fileList.value = [{ 
      name: file.name, 
      url: URL.createObjectURL(file), // 创建本地预览URL
      raw: file, // 保存原始File对象
      uid: Date.now() 
    }]
    isImageChanged.value = true // 标记图片已修改
    onSuccess?.('success')
  } catch (err) {
    ElMessage.error('图片读取失败，请重新上传')
    onError?.(err)
  }
}

/**
 * 上传前校验
 */
const beforeUpload = (file) => {
  const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('仅支持 JPG/PNG/GIF/WEBP 格式的图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

/**
 * 移除图片
 */
const removeImage = (file) => {
  fileList.value = fileList.value.filter(f => f.uid !== file.uid)
  if (file.raw) {
    // 释放URL对象
    URL.revokeObjectURL(file.url)
  }
}

/**
 * 预览图片
 */
const previewImage = (file) => {
  previewUrl.value = file.url
  previewVisible.value = true
}

/**
 * 提交表单（新增/编辑）
 */
const submitForm = async () => {
  await formRef.value.validate();
  submitting.value = true;

  const imageFile = fileList.value.find(item => item.raw)?.raw || null;
  let res;

  if (dialogType.value === 'add') {
    res = await addAdvertisement(form, imageFile);
  } else {
    // 如果没有换图片而且表单里有原图，就传 oldImageUrl
    const oldImageUrl = !isImageChanged.value && form.imageUrl ? form.imageUrl : null;
    res = await updateAdvertisement(form, imageFile, oldImageUrl);
  }

  if (res.code === 200) {
    ElMessage.success(dialogType.value === 'add' ? '新增成功' : '更新成功')
    dialogVisible.value = false
    fetchList() // 重新获取列表
  } else {
    ElMessage.error(res.msg || (dialogType.value === 'add' ? '新增失败' : '更新失败'))
  }
}

/**
 * 切换广告状态（启用/禁用）
 */
const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  
  try {
    await ElMessageBox.confirm(`确定要${action}该广告吗？`, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 调用更新接口，只更新状态
    const res = await updateAdvertisement({
      id: row.id,
      title: row.title,
      linkUrl: row.linkUrl,
      movieId: row.movieId,
      status: newStatus,
      sortOrder: row.sortOrder,
      isDeleted: row.isDeleted
    }, null, row.imageUrl)
    
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      fetchList() // 重新获取列表
    } else {
      ElMessage.error(res.msg || `${action}失败`)
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('handleToggleStatus error:', err)
    }
  }
}

/**
 * 删除广告
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该广告吗？删除后无法恢复。', '删除提示', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await deleteAdvertisement(row.id)
    
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchList() // 重新获取列表
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('handleDelete error:', err)
    }
  }
}
</script>

<style scoped>
.advertisement-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.header-card {
  padding: 20px 25px;
  margin-bottom: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: none;
}

.header-card h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-card {
  padding: 20px 25px;
  margin-bottom: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: none;
}

.search-card :deep(.el-form) {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
  align-items: center;
}

.search-card :deep(.el-form-item) {
  margin-bottom: 0 !important;
}

.table-card {
  padding: 20px 25px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: none;
}

.table-card :deep(.el-table) {
  border-radius: 4px;
  overflow: hidden;
}

.table-card :deep(.el-table__header) {
  background: #f5f7fa;
}

.table-card :deep(.el-table__header th) {
  background-color: #f5f7fa;
  color: #303133;
  font-weight: 600;
  border-bottom: 1px solid #ebeef5;
}

.table-card :deep(.el-table__body tr:hover > td) {
  background-color: #f5f7fa !important;
}

.ad-thumb {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.ad-thumb:hover {
  transform: scale(1.05);
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.pagination :deep(.el-pagination) {
  background: transparent;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}

/* 对话框美化 */
:deep(.el-dialog) {
  border-radius: 8px;
}

:deep(.el-dialog__header) {
  padding: 20px 25px;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

:deep(.el-dialog__body) {
  padding: 25px;
}

:deep(.el-dialog__footer) {
  padding: 15px 25px;
  border-top: 1px solid #ebeef5;
  text-align: right;
}

/* 表单输入框美化 */
:deep(.el-input__wrapper) {
  border-radius: 4px;
}

:deep(.el-input__inner) {
  border-radius: 4px;
}

:deep(.el-select__wrapper) {
  border-radius: 4px;
}

:deep(.el-button) {
  border-radius: 4px;
  font-weight: 500;
}

/* 非链接按钮（新增等）有过渡和悬停效果 */
:deep(.el-button:not(.is-link)) {
  transition: all 0.3s ease;
}

:deep(.el-button--primary:not(.is-link)) {
  background-color: #409eff;
}

:deep(.el-button--primary:not(.is-link):hover) {
  background-color: #66b1ff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.3);
}

:deep(.el-button--danger:not(.is-link)) {
  background-color: #f56c6c;
}

:deep(.el-button--danger:not(.is-link):hover) {
  background-color: #f89898;
}

:deep(.el-button--warning:not(.is-link)) {
  background-color: #e6a23c;
}

:deep(.el-button--warning:not(.is-link):hover) {
  background-color: #ebb563;
}

/* 链接按钮（操作列）始终显示，无动画 */
:deep(.el-button.is-link) {
  transition: none;
}

:deep(.el-button.is-link.is-text) {
  color: #409eff;
}

:deep(.el-button.is-link.is-text.is-danger) {
  color: #f56c6c;
}

:deep(.el-button.is-link.is-text.is-warning) {
  color: #e6a23c;
}

:deep(.el-button.is-link.is-text:hover) {
  opacity: 0.8;
}

:deep(.el-tag--success) {
  background-color: #f0f9ff;
  color: #67c23a;
  border: 1px solid #c6e2ff;
}

:deep(.el-tag--danger) {
  background-color: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fde2e2;
}

/* 上传组件美化 */
:deep(.el-upload-dragger) {
  border-radius: 6px;
  padding: 40px;
  border: 2px dashed #dcdfe6;
}

:deep(.el-upload-dragger:hover) {
  border-color: #409eff;
  background-color: #f5f7fa;
}

:deep(.el-upload-list__item) {
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease;
}

:deep(.el-upload-list__item:hover) {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

/* 元素样式处理 */
:deep(.text-gray) {
  color: #909399;
}

/* 响应式处理 */
@media (max-width: 768px) {
  .advertisement-container {
    padding: 12px;
  }

  .header-card,
  .search-card,
  .table-card {
    padding: 15px;
  }

  .header-content {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .search-card :deep(.el-form) {
    flex-direction: column;
  }

  .search-card :deep(.el-form-item) {
    width: 100%;
  }

  .ad-thumb {
    width: 60px;
    height: 60px;
  }
}
</style>