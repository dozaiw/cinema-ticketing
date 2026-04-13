<template>
  <div class="hall-manage">
    <!-- 顶部影院选择 -->
    <el-card class="cinema-select-card">
      <el-form :inline="true">
        <el-form-item label="选择影院">
          <el-select
            ref="cinemaSelectRef"
            v-model="selectedCinemaId"
            placeholder="请先选择影院"
            style="width: 350px"
            filterable
            clearable
            @change="handleCinemaChange"
          >
            <el-option
              v-for="item in cinemaList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            >
              <span style="float: left">{{ item.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 12px">
                {{ item.address }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            :disabled="!selectedCinemaId"
            @click="handleRefresh"
          >
            🔁 刷新
          </el-button>
          <el-button 
            type="success" 
            :disabled="!selectedCinemaId"
            @click="handleAdd"
          >
            + 新增影厅
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 未选择影院时的提示 -->
    <el-empty 
      v-if="!selectedCinemaId"
      description="请先选择影院以查看影厅列表"
      :image-size="200"
    >
      <el-button type="primary" @click="cinemaSelectRef?.focus()">选择影院</el-button>
    </el-empty>

    <!-- 影厅列表（选中影院后显示） -->
    <el-card v-else class="table-card" v-loading="loading">
      <div class="table-header">
        <h3>📍 {{ selectedCinemaName }} - 影厅列表</h3>
        <el-tag type="info">共 {{ hallList.length }} 个影厅</el-tag>
      </div>
      
      <el-table :data="hallList" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="影厅名称" min-width="150" />
        <el-table-column prop="hallType" label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getHallTypeTag(row.hallType)">
              {{ row.hallType || '普通厅' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="座位规模" width="150" align="center">
          <template #default="{ row }">
            {{ row.seatRows }}行 × {{ row.seatCols }}列<br>
            <small class="text-gray">共 {{ row.seatRows * row.seatCols }} 座</small>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleInitSeats(row)">
              🪑 座位初始化
            </el-button>
            <el-button size="small" type="primary" link @click="handleEdit(row)">
              ✏️ 编辑
            </el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">
              🗑️ 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑影厅' : '新增影厅'"
      width="500px"
      :close-on-click-modal="false"
      @closed="handleDialogClose"
    >
      <el-form ref="hallFormRef" :model="hallForm" :rules="rules" label-width="100px">
        
        <!-- 所属影院（只读） -->
        <el-form-item label="所属影院">
          <el-input :value="selectedCinemaName" disabled />
        </el-form-item>
        
        <!-- 影厅名称 -->
        <el-form-item label="影厅名称" prop="name">
          <el-input 
            v-model="hallForm.name" 
            placeholder="如：1号厅、IMAX激光厅"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        
        <!-- 影厅类型 -->
        <!-- <el-form-item label="影厅类型" prop="hallType">
          <el-select v-model="hallForm.hallType" placeholder="请选择类型" style="width: 100%">
            <el-option label="普通厅" value="普通厅" />
            <el-option label="IMAX厅" value="IMAX厅" />
            <el-option label="杜比全景声厅" value="杜比全景声厅" />
            <el-option label="VIP厅" value="VIP厅" />
            <el-option label="4D厅" value="4D厅" />
            <el-option label="巨幕厅" value="巨幕厅" />
          </el-select>
        </el-form-item> -->
        
        <!-- 座位规模 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="行数" prop="seatRows">
              <el-input-number 
                v-model="hallForm.seatRows" 
                :min="1" 
                :max="50" 
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="列数" prop="seatCols">
              <el-input-number 
                v-model="hallForm.seatCols" 
                :min="1" 
                :max="30" 
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <div class="form-tip">
            💡 总座位数：{{ hallForm.seatRows * hallForm.seatCols }} 座
          </div>
        </el-form-item>
        
        <!-- 状态 -->
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="hallForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCinemaList } from '@/api/cinema'
import { 
  getHallsByCinema, 
  addHall, 
  updateHall, 
  deleteHall,
  initHallSeats 
} from '@/api/hall'

// 数据
const cinemaList = ref([])
const hallList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const hallFormRef = ref(null)
const cinemaSelectRef = ref(null)

// 影院选择
const selectedCinemaId = ref(null)
const selectedCinemaName = computed(() => {
  const cinema = cinemaList.value.find(c => c.id === selectedCinemaId.value)
  return cinema?.name || ''
})

// 影厅表单
const hallForm = reactive({
  id: null,
  cinemaId: null,
  name: '',
  hallType: '普通厅',
  seatRows: 10,
  seatCols: 15,
  status: 1
})

// 验证规则
const rules = {
  name: [
    { required: true, message: '请输入影厅名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  // hallType: [
  //   { required: true, message: '请选择影厅类型', trigger: 'change' }
  // ],
  seatRows: [
    { required: true, message: '请输入行数', trigger: 'blur' }
  ],
  seatCols: [
    { required: true, message: '请输入列数', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 获取影院列表
const fetchCinemaList = async () => {
  try {
    const res = await getCinemaList({ pageNum: 1, pageSize: 100 })
    cinemaList.value = res.data?.records || res.data || []
  } catch (error) {
    console.error('获取影院列表失败:', error)
    ElMessage.error('获取影院列表失败')
  }
}

// 获取影厅列表（根据影院ID）
const fetchHallList = async (cinemaId) => {
  if (!cinemaId) return
  loading.value = true
  try {
    const res = await getHallsByCinema(cinemaId)
    hallList.value = res.data || []
  } catch (error) {
    console.error('获取影厅列表失败:', error)
    ElMessage.error('获取影厅列表失败')
    hallList.value = []
  } finally {
    loading.value = false
  }
}

// 影院选择变化
const handleCinemaChange = (cinemaId) => {
  if (cinemaId) {
    fetchHallList(cinemaId)
  } else {
    hallList.value = []
  }
}

// 刷新当前影院影厅列表
const handleRefresh = () => {
  if (selectedCinemaId.value) {
    fetchHallList(selectedCinemaId.value)
    ElMessage.success('刷新成功')
  }
}

// 获取影厅类型标签样式
const getHallTypeTag = (type) => {
  const map = {
    '普通厅': 'info',
    'IMAX厅': 'danger',
    '杜比全景声厅': 'warning',
    'VIP厅': 'success',
    '4D厅': '',
    '巨幕厅': ''
  }
  return map[type] || 'info'
}

// 新增影厅
const handleAdd = () => {
  if (!selectedCinemaId.value) {
    ElMessage.warning('请先选择影院')
    return
  }
  isEdit.value = false
  Object.assign(hallForm, {
    id: null,
    cinemaId: selectedCinemaId.value,
    name: '',
    hallType: '普通厅',
    seatRows: 10,
    seatCols: 15,
    status: 1
  })
  dialogVisible.value = true
}

// 编辑影厅
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(hallForm, {
    id: row.id,
    cinemaId: row.cinemaId,
    name: row.name,
    hallType: row.hallType || '普通厅',
    seatRows: row.seatRows,
    seatCols: row.seatCols,
    status: row.status
  })
  dialogVisible.value = true
}

// 删除影厅
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除影厅「${row.name}」吗？\n删除后该影厅的排片数据将不可用！`,
    '警告',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      await deleteHall(row.id)
      ElMessage.success('删除成功')
      fetchHallList(selectedCinemaId.value)
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

// 初始化座位
const handleInitSeats = (row) => {
  ElMessageBox.confirm(
    `确定要初始化「${row.name}」的座位吗？\n⚠️ 此操作将清空该影厅所有现有座位数据！`,
    '确认初始化',
    {
      confirmButtonText: '确认初始化',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      await initHallSeats(row.id)
      ElMessage.success('座位初始化成功')
    } catch (error) {
      console.error('初始化失败:', error)
      ElMessage.error(error.message || '初始化失败')
    }
  }).catch(() => {})
}

// 提交表单
const handleSubmit = async () => {
  try {
    await hallFormRef.value?.validate()
  } catch {
    return
  }
  
  submitLoading.value = true
  try {
    const formData = {
      id: hallForm.id,
      name: hallForm.name,
      hallType: hallForm.hallType,
      seatRows: hallForm.seatRows,
      seatCols: hallForm.seatCols,
      status: hallForm.status
    }
    
    if (isEdit.value) {
      // 编辑：调用更新接口
      await updateHall(formData)
      ElMessage.success('更新成功')
    } else {
      // 新增：调用新增接口（cinemaId 从 URL 传入）
      await addHall(selectedCinemaId.value, {
        name: formData.name,
        hallType: formData.hallType,
        seatRows: formData.seatRows,
        seatCols: formData.seatCols
      })
      ElMessage.success('添加成功')
    }
    
    dialogVisible.value = false
    fetchHallList(selectedCinemaId.value)
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 对话框关闭
const handleDialogClose = () => {
  hallFormRef.value?.resetFields()
}

// 页面初始化
onMounted(() => {
  fetchCinemaList()
})
</script>

<style scoped>
.hall-manage {
  padding: 20px;
}
.cinema-select-card {
  margin-bottom: 20px;
}
.table-card {
  min-height: 400px;
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}
.table-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}
.text-gray {
  color: #909399;
  font-size: 12px;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
:deep(.el-empty) {
  padding: 60px 0;
}
</style>