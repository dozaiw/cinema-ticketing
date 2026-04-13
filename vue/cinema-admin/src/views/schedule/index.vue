<template>
  <div class="schedule-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="选择电影">
          <el-select 
            v-model="searchForm.movieId" 
            placeholder="全部" 
            clearable 
            style="width: 200px"
          >
            <el-option v-for="item in movieList" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择影院">
          <el-select 
            v-model="searchForm.cinemaId" 
            placeholder="全部" 
            clearable 
            style="width: 200px"
          >
            <el-option v-for="item in cinemaList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排片日期">
          <el-date-picker 
            v-model="searchForm.date" 
            type="date" 
            placeholder="全部" 
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部"
            clearable
            style="width: 150px"
          >
            <el-option label="未开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleResetSearch">重置</el-button>
          <el-button type="success" @click="handleAdd">+ 新增排片</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card">
      <el-table :data="scheduleList" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        
        <!-- 🔥 修改：使用 movieTitle 而不是 movieName -->
        <el-table-column prop="movieTitle" label="电影名称" min-width="150" show-overflow-tooltip />
        
        <el-table-column prop="hallName" label="影厅" width="120" />
        <el-table-column prop="cinemaName" label="影院" min-width="150" />
        
        <!-- 🔥 修改：显示格式化后的时间 -->
        <el-table-column label="开始时间" width="160" align="center">
          <template #default="{ row }">
            {{ row.startTime ? row.startTime.substring(0, 16) : '-' }}
          </template>
        </el-table-column>
        
        <!-- <el-table-column label="结束时间" width="180" align="center">
          <template #default="{ row }">
            {{ row.endTimeStr || (row.endTime ? row.endTime.substring(0, 16) : '') }}
          </template>
        </el-table-column> -->
        
        <el-table-column prop="price" label="票价 (元)" width="100" align="center">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        
        <!-- 🔥 修改：使用动态状态（displayStatus） -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.displayStatus)" effect="light">
              {{ row.displayStatusDesc || getStatusText(row.displayStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <!-- 🔥 根据 isEditable/isDeletable 控制按钮显示 -->
            <el-button 
              v-if="row.isEditable !== false" 
              size="small" 
              type="primary" 
              link 
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button 
              v-if="row.isDeletable !== false" 
              size="small" 
              type="danger" 
              link 
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <!-- 如果不可编辑/删除，显示状态提示 -->
            <span v-if="row.isEditable === false && row.isDeletable === false" style="color: #909399; font-size: 12px;">
              {{ row.displayStatusDesc || '只读' }}
            </span>
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
          @size-change="fetchScheduleList"
          @current-change="fetchScheduleList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框（保持不变） -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑排片' : '新增排片'"
      width="550px"
      :close-on-click-modal="false"
      @closed="handleDialogClose"
    >
      <el-form ref="scheduleFormRef" :model="scheduleForm" :rules="rules" label-width="100px">
        
        <el-form-item label="选择影院" prop="cinemaId">
          <el-select 
            v-model="scheduleForm.cinemaId" 
            placeholder="请选择影院" 
            style="width: 100%"
            @change="handleCinemaChange"
            :disabled="isEdit"
          >
            <el-option v-for="item in cinemaList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="选择电影" prop="movieId">
          <el-select v-model="scheduleForm.movieId" placeholder="选择电影" style="width: 100%">
            <el-option v-for="item in movieList" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="选择影厅" prop="hallId">
          <el-select 
            v-model="scheduleForm.hallId" 
            placeholder="请先选择影院" 
            style="width: 100%"
            :disabled="!scheduleForm.cinemaId"
          >
            <el-option v-for="item in hallList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker 
            v-model="scheduleForm.startTime" 
            placeholder="选择开始时间" 
            style="width: 100%"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        
        <!-- <el-form-item label="结束时间" prop="endTime">
          <el-date-picker 
            v-model="scheduleForm.endTime" 
            type="datetime" 
            placeholder="选择结束时间" 
            style="width: 100%"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item> -->
        
        <el-form-item label="票价 (元)" prop="price">
          <el-input-number v-model="scheduleForm.price" :min="0" :max="1000" />
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
import { 
  addSchedule, 
  updateSchedule, 
  deleteSchedule, 
  getSchedulesByMovieCinemaAndDate, 
  getAllSchedule, 
  getFilteredScheduleList
} from '@/api/schedule'
import { getHotMovies } from '@/api/movie'
import { getCinemaList } from '@/api/cinema'
import { getHallsByCinema } from '@/api/hall'

// 表格数据
const scheduleList = ref([])
const movieList = ref([])
const cinemaList = ref([])
const hallList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 对话框相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const scheduleFormRef = ref(null)

// 搜索表单
const searchForm = reactive({
  movieId: null,
  cinemaId: null,
  date: null,
  status: ''        // 业务状态 0 未开始 1 进行中 2 已结束
})

// 新增/编辑表单
const scheduleForm = reactive({
  id: null,
  cinemaId: null,
  movieId: null,
  hallId: null,
  startTime: '',
  // endTime: '',
  price: 50
})

// 表单验证规则
const rules = {
  cinemaId: [{ required: true, message: '请选择影院', trigger: 'change' }],
  movieId: [{ required: true, message: '请选择电影', trigger: 'change' }],
  hallId: [{ required: true, message: '请选择影厅', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  // endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  price: [{ required: true, message: '请输入票价', trigger: 'blur' }]
}

// 🔥 新增：状态映射工具函数
const getStatusText = (status) => {
  const map = { 0: '未开始', 1: '进行中', 2: '已结束' }
  return map[status] || '未知'
}

const getStatusTagType = (status) => {
  const map = { 0: 'success', 1: 'warning', 2: 'danger' }
  return map[status] || 'info'
}

// 获取电影列表
const fetchMovieList = async () => {
  try {
    const res = await getHotMovies({ pageSize: 100 })
    movieList.value = res.data?.records || res.data || []
  } catch (error) {
    console.error('获取电影列表失败:', error)
    ElMessage.error('获取电影列表失败')
  }
}

// 获取影院列表
const fetchCinemaList = async () => {
  try {
    const res = await getCinemaList({ pageSize: 100 })
    cinemaList.value = res.data?.records || res.data || []
  } catch (error) {
    console.error('获取影院列表失败:', error)
    ElMessage.error('获取影院列表失败')
  }
}

// 根据影院ID获取影厅列表
const fetchHallList = async (cinemaId) => {
  if (!cinemaId) {
    hallList.value = []
    return
  }
  try {
    const res = await getHallsByCinema(cinemaId)
    hallList.value = res.data || []
  } catch (error) {
    console.error('获取影厅列表失败:', error)
    ElMessage.error('获取影厅列表失败')
    hallList.value = []
  }
}

// 获取排片列表
const fetchScheduleList = async () => {
  loading.value = true
  try {
    const params = {
    pageNum: currentPage.value,
    pageSize: pageSize.value,
      movieId: searchForm.movieId || undefined,
      cinemaId: searchForm.cinemaId || undefined,
    date: searchForm.date ? new Date(searchForm.date).toISOString().split('T')[0] : undefined,
      status: (searchForm.status !== '' && searchForm.status != null) ? Number(searchForm.status) : undefined
    }

    const res = await getFilteredScheduleList(params)

    if (res.data?.records) {
      scheduleList.value = res.data.records
      total.value = res.data.total || res.data.records.length
    } else if (Array.isArray(res.data)) {
      scheduleList.value = res.data
      total.value = res.data.length
    } else {
      scheduleList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取排片列表失败:', error)
    ElMessage.error(error.message || '获取排片列表失败')
    scheduleList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索按钮点击
const handleSearch = () => {
  currentPage.value = 1
  fetchScheduleList()
}

// 重置所有查询条件
const handleResetSearch = () => {
  searchForm.movieId = null
  searchForm.cinemaId = null
  searchForm.date = null
  searchForm.status = ''
}
// 新增排片
const handleAdd = () => {
  isEdit.value = false
  Object.assign(scheduleForm, {
    id: null,
    cinemaId: null,
    movieId: null,
    hallId: null,
    startTime: '',
    // endTime: '',
    price: 50
  })
  hallList.value = []
  dialogVisible.value = true
}

// 编辑排片
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(scheduleForm, {
    id: row.id,
    cinemaId: row.cinemaId,
    movieId: row.movieId,
    hallId: row.hallId,
    startTime: row.startTime,
    // endTime: row.endTime,
    price: row.price
  })
  if (row.cinemaId) {
    fetchHallList(row.cinemaId)
  }
  dialogVisible.value = true
}

// 删除排片
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除该排片吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteSchedule(row.id)
      ElMessage.success('删除成功')
      fetchScheduleList()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

// 影院选择变化时，动态加载影厅
const handleCinemaChange = (cinemaId) => {
  scheduleForm.hallId = null
  fetchHallList(cinemaId)
}

// 对话框关闭时重置
const handleDialogClose = () => {
  scheduleFormRef.value?.resetFields()
  hallList.value = []
}

// 提交表单
const handleSubmit = async () => {
  try {
    await scheduleFormRef.value?.validate()
  } catch {
    return
  }
  
  submitLoading.value = true
  try {
    const formData = {
      cinemaId: scheduleForm.cinemaId,
      hallId: scheduleForm.hallId,
      movieId: scheduleForm.movieId,
      startTime: scheduleForm.startTime,
      endTime: null,
      price: scheduleForm.price
    }
    
    if (isEdit.value) {
      formData.id = scheduleForm.id
      await updateSchedule(formData)
      ElMessage.success('更新成功')
    } else {
      await addSchedule(formData)
      ElMessage.success('添加成功')
    }
    
    dialogVisible.value = false
    fetchScheduleList()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 页面挂载时初始化
onMounted(() => {
  fetchMovieList()
  fetchCinemaList()
  fetchScheduleList()
})
</script>

<style scoped>
.schedule-manage {
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
</style>