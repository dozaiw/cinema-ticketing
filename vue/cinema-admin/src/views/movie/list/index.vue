<template>
  <div class="movie-list">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="电影名称">
          <el-input 
            v-model="searchForm.title" 
            placeholder="请输入电影名称" 
            clearable 
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="电影类型">
          <el-select 
            v-model="searchForm.genre" 
            placeholder="选择类型" 
            clearable
            style="width: 150px"
          >
            <el-option v-for="item in genreList" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select 
            v-model="searchForm.status" 
            placeholder="全部" 
            clearable
            style="width: 120px"
          >
            <el-option label="热映" :value="1" />
            <el-option label="待映" :value="2" />
            <el-option label="已下映" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">+ 新增电影</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card">
      <el-table 
        :data="movieList" 
        border 
        stripe 
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        
        <!-- 海报列 -->
        <el-table-column label="海报" width="100" align="center">
          <template #default="{ row }">
            <el-image 
              v-if="row.poster"
              :src="row.poster" 
              :preview-src-list="[row.poster]"
              fit="cover"
              style="width: 60px; height: 80px; border-radius: 4px"
            >
              <template #error>
                <div class="image-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div v-else class="no-poster">无海报</div>
          </template>
        </el-table-column>
        
        <el-table-column prop="title" label="电影名称" min-width="150" />
        <el-table-column prop="duration" label="时长 (分钟)" width="100" align="center" />
        <el-table-column label="上映日期" width="120" align="center">
          <template #default="{ row }">
            {{ formatDate(row.releaseDate) }}
          </template>
        </el-table-column>
        <el-table-column label="下映日期" width="120" align="center">
          <template #default="{ row }">
            {{ formatDate(row.offlineDate) }}
          </template>
        </el-table-column>
        
        <!-- 主演列 -->
        <el-table-column label="主演" min-width="200">
          <template #default="{ row }">
            <el-tag 
              v-for="actor in getMainActors(row.staffList)" 
              :key="actor.actorId" 
              size="small" 
              style="margin: 2px"
            >
              {{ actor.actorName }}
            </el-tag>
            <el-tag 
              v-if="getMainActors(row.staffList)?.length > 3" 
              size="small" 
              type="info"
              style="margin: 2px"
            >
              +{{ getMainActors(row.staffList).length - 3 }}
            </el-tag>
            <span v-if="!getMainActors(row.staffList)?.length" class="text-gray">-</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="简介" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑电影' : '新增电影'"
      width="750px"
      @closed="handleDialogClose"
    >
      <el-form ref="movieFormRef" :model="movieForm" :rules="rules" label-width="100px">
        <el-form-item label="电影名称" prop="title">
          <el-input v-model="movieForm.title" placeholder="请输入电影名称" />
        </el-form-item>
        
        <!-- 电影类型选择（存 ID 数组） -->
        <el-form-item label="电影类型" prop="genreIds">
          <el-select 
            v-model="movieForm.genreIds" 
            multiple 
            placeholder="选择类型" 
            style="width: 100%"
            :collapse-tags="true"
            :collapse-tags-tooltip="true"
          >
            <el-option 
              v-for="item in genreList" 
              :key="item.id" 
              :label="item.name" 
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        
        <!-- 主演演员选择 -->
        <el-form-item label="主演演员">
          <el-select
            v-model="movieForm.actorIds"
            multiple
            filterable
            remote
            reserve-keyword
            placeholder="搜索并选择演员"
            :remote-method="handleActorSearch"
            :loading="actorLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in actorOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            >
              <span style="float: left">{{ item.name }}</span>
            </el-option>
          </el-select>
          <div class="form-tip">可多选，支持搜索，最多选择 10 位主演</div>
        </el-form-item>
        
        <el-form-item label="时长 (分钟)" prop="duration">
          <el-input-number v-model="movieForm.duration" :min="60" :max="300" />
        </el-form-item>
        
        <el-form-item label="上映日期" prop="releaseDate">
          <el-date-picker 
            v-model="movieForm.releaseDate" 
            type="date" 
            placeholder="选择日期" 
            style="width: 100%" 
            value-format="YYYY-MM-DD" 
          />
        </el-form-item>
        
        <el-form-item label="下映日期" prop="offlineDate">
          <el-date-picker 
            v-model="movieForm.offlineDate" 
            type="date" 
            placeholder="选择日期" 
            style="width: 100%" 
            value-format="YYYY-MM-DD" 
          />
        </el-form-item>
        
        <el-form-item label="电影简介" prop="description">
          <el-input 
            v-model="movieForm.description" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入电影简介" 
          />
        </el-form-item>
        
        <!-- 海报上传（带预览） -->
        <el-form-item label="海报" prop="posterFile">
          <el-upload
            ref="posterUploadRef"
            action="#"
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handlePosterChange"
            :file-list="posterFileList"
          >
            <el-button type="primary">选择海报</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 jpg/png 格式，不超过 5MB</div>
            </template>
          </el-upload>
          <!-- 编辑时显示现有海报 -->
          <div v-if="isEdit && movieForm.poster" class="poster-preview">
            <el-image 
              :src="movieForm.poster" 
              fit="cover"
              style="width: 100px; height: 140px; margin-top: 10px; border-radius: 4px"
            />
            <div class="poster-tip">上传新海报将替换现有海报</div>
          </div>
        </el-form-item>
        
        <!-- 预告片上传 -->
        <el-form-item label="预告片" prop="trailerFile">
          <el-upload
            ref="trailerUploadRef"
            action="#"
            :auto-upload="false"
            :limit="1"
            accept="video/*"
            :on-change="handleTrailerChange"
            :file-list="trailerFileList"
          >
            <el-button type="primary">选择预告片</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 mp4 格式，不超过 100MB</div>
            </template>
          </el-upload>
          <!-- 编辑时显示现有预告片 -->
          <div v-if="isEdit && movieForm.trailerUrl" class="trailer-preview">
            <video 
              :src="movieForm.trailerUrl" 
              controls
              style="width: 100%; max-width: 400px; margin-top: 10px; border-radius: 4px"
            />
            <div class="trailer-tip">上传新预告片将替换现有预告片</div>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { 
  changeMovieState, 
  addMovie, updateMovie, getAllMovie, deleteMovie 
} from '@/api/movie'
import { getGenreList } from '@/api/genre'
import { searchActor } from '@/api/actor'
import { batchAddMovieStaff, getStaffByMovie, deleteMovieStaff } from '@/api/movieStaff'

// 数据
const movieList = ref([])
const genreList = ref([])  // 🔥 类型列表（用于转换 ID→对象）
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const movieFormRef = ref(null)
const posterUploadRef = ref(null)
const trailerUploadRef = ref(null)

// 文件列表（用于上传组件显示）
const posterFileList = ref([])
const trailerFileList = ref([])

// 演员相关
const actorOptions = ref([])
const actorLoading = ref(false)

const searchForm = reactive({
  title: '',
  genre: '',
  status: null   // 使用 null 代表未选择
})

// 🔥 表单数据：genreIds 存 ID 数组 [1,2,3]
const movieForm = reactive({
  id: null,
  title: '',
  description: '',
  duration: 120,
  releaseDate: '',
  offlineDate: '',
  genreIds: [],        // ✅ 存储类型 ID 数组
  actorIds: [],
  poster: '',
  posterFile: null,
  trailerUrl: '',
  trailerFile: null,
  status: 1
})

const rules = {
  title: [
    { required: true, message: '请输入电影名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  genreIds: [
    { required: true, message: '请至少选择一个电影类型', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入电影简介', trigger: 'blur' }
  ],
  duration: [
    { required: true, message: '请输入时长', trigger: 'blur' }
  ],
  releaseDate: [
    { required: true, message: '请选择上映日期', trigger: 'change' }
  ]
}

// 获取主演列表
const getMainActors = (staffList) => {
  if (!staffList || !Array.isArray(staffList)) return []
  return staffList.filter(item => item.role === '主演')
}

// 搜索演员
const handleActorSearch = async (query) => {
  if (!query) {
    actorOptions.value = []
    return
  }
  actorLoading.value = true
  try {
    const res = await searchActor(query)
    actorOptions.value = res.data || []
  } catch (error) {
    console.error('搜索演员失败:', error)
    ElMessage.error('搜索演员失败')
    actorOptions.value = []
  } finally {
    actorLoading.value = false
  }
}

// 获取电影关联的演员
const fetchMovieStaff = async (movieId) => {
  if (!movieId) return []
  try {
    const res = await getStaffByMovie(movieId)
    return res.data?.staffList || []
  } catch (error) {
    console.error('获取演职人员失败:', error)
    return []
  }
}

// 清理主演关联
const clearMainActors = async (movieId) => {
  try {
    const res = await getStaffByMovie(movieId)
    const staffList = res.data?.staffList || []
    const mainActors = staffList.filter(item => item.role === '主演')
    for (const item of mainActors) {
      await deleteMovieStaff(movieId, item.actorId)
    }
  } catch (error) {
    console.warn('清理演员关联失败:', error)
  }
}

// 获取电影列表（始终调用同一接口，其他筛选在前端完成）
const fetchMovieList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      title: searchForm.title || undefined,
      genre: searchForm.genre || undefined
    }
    const res = await getAllMovie(params)
    let records = res.data.records || []

    // 前端状态筛选
    if (searchForm.status !== '' && searchForm.status != null) {
      // 有时 status 可能是数字或字符串，统一比较
      records = records.filter(item => String(item.status) === String(searchForm.status))
    }

    movieList.value = records
    total.value = records.length
  } catch (error) {
    console.error(error)
    ElMessage.error('获取电影列表失败')
  } finally {
    loading.value = false
  }
}

// 获取类型列表
// MovieManage.vue - 修改 fetchGenreList

const fetchGenreList = async () => {
  try {
    const res = await getGenreList()
    
    // 🔥 关键修复：兼容多种返回格式
    if (Array.isArray(res.data)) {
      // 格式1: 直接返回数组 [ {...}, {...} ]
      genreList.value = res.data
    } else if (res.data?.records && Array.isArray(res.data.records)) {
      // 格式2: 分页格式 { records: [...], total: 10 }
      genreList.value = res.data.records
    } else if (res.data?.list && Array.isArray(res.data.list)) {
      // 格式3: { list: [...] }
      genreList.value = res.data.list
    } else {
      // 兜底：空数组
      genreList.value = []
      console.warn('⚠️ 类型列表数据格式未知:', res.data)
    }
    
    console.log('✅ 类型列表加载成功:', genreList.value.length, '项')
    
  } catch (error) {
    console.error('❌ 获取类型列表失败:', error)
    ElMessage.error('获取电影类型失败')
    genreList.value = []
  }
}

// 搜索（重用 fetchMovieList，包含状态过滤）
const handleSearch = async () => {
  pageNum.value = 1
  fetchMovieList()
}

// 重置
const handleReset = () => {
  searchForm.title = ''
  searchForm.genre = ''
  searchForm.status = null
  pageNum.value = 1
  fetchMovieList()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  Object.assign(movieForm, {
    id: null,
    title: '',
    description: '',
    duration: 120,
    releaseDate: '',
    offlineDate: '',
    genreIds: [],
    actorIds: [],
    poster: '',
    posterFile: null,
    trailerUrl: '',
    trailerFile: null,
    status: 1
  })
  posterFileList.value = []
  trailerFileList.value = []
  actorOptions.value = []
  dialogVisible.value = true
}

// 编辑（回填数据）
const handleEdit = async (row) => {
  isEdit.value = true
  
  // 类型回填：根据后端返回格式转换为 ID 数组
  let genreIds = []
  if (Array.isArray(row.genres)) {
    if (row.genres.length > 0 && typeof row.genres[0] === 'object') {
      genreIds = row.genres.map(g => g.id)
    } else if (typeof row.genres[0] === 'number') {
      genreIds = row.genres
    } else {
      genreIds = row.genres.map(name => {
        const found = genreList.value.find(g => g.name === name)
        return found?.id
      }).filter(id => id != null)
    }
  }
  
  Object.assign(movieForm, {
    id: row.id,
    title: row.title,
    description: row.description,
    duration: row.duration,
    releaseDate: row.releaseDate,
    offlineDate: row.offlineDate,
    genreIds: genreIds,
    status: row.status,
    actorIds: [],
    poster: row.poster || '',
    trailerUrl: row.trailerUrl || '',
    posterFile: null,
    trailerFile: null
  })
  
  // 回填演员
  const staffList = await fetchMovieStaff(row.id)
  movieForm.actorIds = getMainActors(staffList).map(item => item.actorId)
  
  // 回填文件列表
  if (row.poster) {
    posterFileList.value = [{ name: '海报', url: row.poster }]
  } else {
    posterFileList.value = []
  }
  if (row.trailerUrl) {
    trailerFileList.value = [{ name: '预告片', url: row.trailerUrl }]
  } else {
    trailerFileList.value = []
  }
  
  dialogVisible.value = true
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除 "${row.title}" 吗？删除后无法恢复！`, '警告', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await deleteMovie(row.id)
      ElMessage.success('删除成功')
      fetchMovieList()
    } catch (error) {
      console.error(error)
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

// 🔥 修复：提交表单（关键：genres 传对象数组）
const handleSubmit = async () => {
  await movieFormRef.value?.validate()
  
  submitLoading.value = true
  try {
    const formData = new FormData()
    
    // ✅ 1. movie JSON：不要传 genreIds（Movie 实体没有这个字段）
    formData.append('movie', JSON.stringify({
      id: movieForm.id,
      title: movieForm.title,
      description: movieForm.description,
      duration: movieForm.duration,
      releaseDate: movieForm.releaseDate,
      offlineDate: movieForm.offlineDate,
      status: movieForm.status
      // ❌ 不要传 genreIds，避免 UnrecognizedPropertyException
    }))
    
    // 🔥 2. 【关键】genres part：传 Genre 对象数组（匹配后端 List<Genre>）
    const genreObjects = (movieForm.genreIds || [])
      .map(id => genreList.value.find(g => g.id === id))
      .filter(g => g != null)
    
    formData.append('genres', JSON.stringify(genreObjects))
    
    // 🔥 3. actorIds part（如果后端需要）
    if (movieForm.actorIds?.length > 0) {
      formData.append('actorIds', JSON.stringify(movieForm.actorIds))
    }
    
    // 4. 文件上传
    if (movieForm.posterFile) {
      formData.append('posterFile', movieForm.posterFile)
    }
    if (movieForm.trailerFile) {
      formData.append('trailerFile', movieForm.trailerFile)
    }
    
    // 5. 旧文件 URL（用于删除）
    if (isEdit.value) {
      formData.append('oldPosterUrl', movieForm.poster || '')
      formData.append('oldTrailerUrl', movieForm.trailerUrl || '')
    }
    
    // 6. 调用 API
    let movieId = movieForm.id
    if (isEdit.value) {
      await updateMovie(formData)
      ElMessage.success('更新成功')
    } else {
      const res = await addMovie(formData)
      movieId = res.data?.id || movieForm.id
    }
    
    // 7. 批量处理演员关联
    if (movieId) {
      await clearMainActors(movieId)
      const selectedIds = movieForm.actorIds?.slice(0, 10) || []
      if (selectedIds.length > 0) {
        const relations = selectedIds.map(actorId => ({
          movieId,
          actorId,
          role: '主演',
          characterName: ''
        }))
        await batchAddMovieStaff(relations)
      }
    }
    
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    fetchMovieList()
    
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 文件选择
const handlePosterChange = (file) => {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('海报大小不能超过 5MB')
    posterUploadRef.value?.clearFiles()
    return
  }
  movieForm.posterFile = file.raw
}

const handleTrailerChange = (file) => {
  if (file.size > 100 * 1024 * 1024) {
    ElMessage.error('预告片大小不能超过 100MB')
    trailerUploadRef.value?.clearFiles()
    return
  }
  movieForm.trailerFile = file.raw
}

// 对话框关闭
const handleDialogClose = () => {
  movieFormRef.value?.resetFields()
  posterFileList.value = []
  trailerFileList.value = []
  actorOptions.value = []
}

// 工具函数
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString()
}

const getStatusType = (status) => {
  const map = { 0: 'info', 1: 'success', 2: 'warning' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  // 后端状态和查询选项使用 0 表示已下映
  const map = { 0: '已下映', 1: '热映', 2: '待映' }
  return map[status] || '未知'
}

// 分页
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchMovieList()
}

const handlePageChange = (val) => {
  pageNum.value = val
  fetchMovieList()
}

onMounted(() => {
  fetchMovieList()
  fetchGenreList()  // 🔥 确保类型列表已加载，用于 ID→对象转换
})
</script>

<style scoped>
.movie-list {
  padding: 20px;
}
.search-card, .table-card {
  margin-bottom: 20px;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.text-gray {
  color: #909399;
}
.no-poster {
  color: #909399;
  font-size: 12px;
}
.image-error {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 60px;
  height: 80px;
  background: #f5f7fa;
  color: #909399;
  border-radius: 4px;
}
.image-error .el-icon {
  font-size: 24px;
}
.poster-preview, .trailer-preview {
  margin-top: 10px;
}
.poster-tip, .trailer-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>