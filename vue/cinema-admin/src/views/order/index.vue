<template>
  <div class="order-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 150px">
            <el-option label="待支付" value="PENDING" />
            <el-option label="已支付" value="PAID" />
            <el-option label="已使用" value="USED" />
            <el-option label="已取消" value="CANCELED" />
            <el-option label="已退款" value="REFUND" />
            <el-option label="已过期" value="EXPIRED" />
          </el-select>
        </el-form-item>
        <el-form-item label="电影名称">
          <el-input v-model="searchForm.movieName" placeholder="请输入电影名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="影厅">
          <el-input v-model="searchForm.hallName" placeholder="请输入影厅名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="放映日期">
          <el-date-picker
            v-model="searchForm.date"
            type="date"
            placeholder="全部"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card">
      <el-table :data="orderList" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="movieName" label="电影名称" min-width="150" />
        <el-table-column prop="hallName" label="影厅" width="120" />
        <el-table-column prop="seatNames" label="座位" width="160">
          <template #default="{ row }">
            {{ formatSeatNames(row.seatNames) }}
          </template>
        </el-table-column>
        <el-table-column prop="showTime" label="放映时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.showTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额 (元)" width="100" align="center">
          <template #default="{ row }">
            {{ (row.totalAmount / 100).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 'PAID' && row.verifyCode"
              size="small"
              type="success"
              link
              @click="handleVerify(row)"
            >
              核销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentOrder.status)">
            {{ getStatusText(currentOrder.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="电影名称">{{ currentOrder.movieName }}</el-descriptions-item>
        <el-descriptions-item label="影厅">{{ currentOrder.hallName }}</el-descriptions-item>
        <el-descriptions-item label="座位">{{ formatSeatNames(currentOrder.seatNames) }}</el-descriptions-item>
        <el-descriptions-item label="放映时间">{{ formatDate(currentOrder.showTime) }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ (currentOrder.totalAmount / 100).toFixed(2) }} 元</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentOrder.userPhone }}</el-descriptions-item>
        <el-descriptions-item v-if="currentOrder.verifyCode" label="验票码" :span="2">{{ currentOrder.verifyCode }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDate(currentOrder.createTime) }}</el-descriptions-item>
      </el-descriptions>
      
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryAllOrder, verifyTicket } from '@/api/order'

const orderList = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const currentOrder = ref(null)

const searchForm = reactive({
  status: '',
  movieName: '',
  hallName: '',
  date: null
})

const fetchOrderList = async () => {
  loading.value = true
  try {
    const res = await queryAllOrder()
    let records = [...(res.data || [])].sort((a, b) => new Date(b.createTime) - new Date(a.createTime))

    // 客户端过滤
    if (searchForm.status) {
      records = records.filter(item => item.status === searchForm.status)
    }
    if (searchForm.movieName) {
      records = records.filter(item =>
        item.movieName && item.movieName.includes(searchForm.movieName)
      )
    }
    if (searchForm.hallName) {
      records = records.filter(item =>
        item.hallName && item.hallName.includes(searchForm.hallName)
      )
    }
    if (searchForm.date) {
      const dateStr = formatDateOnly(searchForm.date)
      records = records.filter(item =>
        item.showTime && formatDateOnly(new Date(item.showTime)) === dateStr
      )
    }

    orderList.value = records
  } catch (error) {
    console.error('获取订单列表失败:', error)
    orderList.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  fetchOrderList()
}

const handleReset = () => {
  searchForm.status = ''
  searchForm.movieName = ''
  searchForm.hallName = ''
  searchForm.date = null
  fetchOrderList()
}

const handleView = (row) => {
  currentOrder.value = row
  detailVisible.value = true
}

const handleVerify = async (row) => {
  await ElMessageBox.confirm(`确定核销订单 "${row.orderNo}" 吗？`, '提示', {
    type: 'warning'
  })

  try {
    await verifyTicket(row.verifyCode)
    ElMessage.success('核销成功')
    fetchOrderList()
  } catch (error) {
    console.error(error)
  }
}

const formatSeatNames = (seatNames) => {
  if (!seatNames) return '-'

  try {
    const parsed = typeof seatNames === 'string' ? JSON.parse(seatNames) : seatNames
    return Array.isArray(parsed) ? parsed.join('、') : seatNames
  } catch (error) {
    return seatNames
  }
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const formatDateOnly = (date) => {
  if (!date) return ''

  const targetDate = date instanceof Date ? date : new Date(date)
  if (Number.isNaN(targetDate.getTime())) return ''

  const year = targetDate.getFullYear()
  const month = String(targetDate.getMonth() + 1).padStart(2, '0')
  const day = String(targetDate.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getStatusType = (status) => {
  const map = {
    'PENDING': 'warning',
    'PAID': 'success',
    'USED': 'success',
    'CANCELED': 'info',
    'REFUND': 'danger',
    'EXPIRED': 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    'PENDING': '待支付',
    'PAID': '已支付',
    'USED': '已使用',
    'CANCELED': '已取消',
    'REFUND': '已退款',
    'EXPIRED': '已过期'
  }
  return map[status] || '未知'
}

onMounted(() => {
  fetchOrderList()
})
</script>

<style scoped>
.order-manage {
  padding: 20px;
}
.search-card, .table-card {
  margin-bottom: 20px;
}
</style>
