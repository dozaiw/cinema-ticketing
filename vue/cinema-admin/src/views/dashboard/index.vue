<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: #409EFF">
            <el-icon><Film /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.movieCount }}</div>
            <div class="stat-label">热映电影</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: #67C23A">
            <el-icon><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.cinemaCount }}</div>
            <div class="stat-label">营业影院</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: #E6A23C">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.scheduleCount }}</div>
            <div class="stat-label">今日排片</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: #F56C6C">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.orderCount }}</div>
            <div class="stat-label">今日订单</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据可视化图表 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 电影销售额占比 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>🎬 今日电影销售额占比</span>
              <el-tag type="success">{{ today }}</el-tag>
            </div>
          </template>
          <div ref="movieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <!-- 影厅销售额占比 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>🏢 今日影院销售额占比</span>
              <el-tag type="success">{{ today }}</el-tag>
            </div>
          </template>
          <div ref="hallChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <!-- 订单状态分布 -->
      <el-col :span="24">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>📊 今日订单状态分布</span>
              <el-tag type="success">{{ today }}</el-tag>
            </div>
          </template>
          <div ref="orderStatusChartRef" class="chart-container-large"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 欢迎信息 -->
    <el-card class="welcome-card">
      <h3>👋 欢迎使用电影院后台管理系统</h3>
      <p>当前用户：<strong>{{ userStore.username }}</strong></p>
      <p>角色：<el-tag>{{ userStore.isAdmin ? '管理员' : '普通用户' }}</el-tag></p>
      <p>登录时间：{{ currentTime }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useUserStore } from '@/store/user'
import { Film, OfficeBuilding, Calendar, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { 
  getHotMovieCount, 
  getWorkingCinemaCount, 
  getTodayScheduleCount, 
  getTodayOrderCount,
  getMovieSalesStats,
  getHallSalesStats,
  getOrderStatusStats
} from '@/api/dashboard'

const userStore = useUserStore()

const stats = ref({
  movieCount: 0,
  cinemaCount: 0,
  scheduleCount: 0,
  orderCount: 0
})

const loading = ref(false)
const today = ref('')

const movieChartRef = ref(null)
const hallChartRef = ref(null)
const orderStatusChartRef = ref(null)

let movieChart = null
let hallChart = null
let orderStatusChart = null

const currentTime = computed(() => {
  return new Date().toLocaleString('zh-CN')
})

const getTodayDate = () => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const loadStats = async () => {
  loading.value = true
  const todayDate = getTodayDate()
  today.value = todayDate
  
  try {
    const [movieRes, cinemaRes, scheduleRes, orderRes] = await Promise.allSettled([
      getHotMovieCount(),
      getWorkingCinemaCount(),
      getTodayScheduleCount(todayDate),
      getTodayOrderCount(todayDate)
    ])
    
    if (movieRes.status === 'fulfilled') {
      stats.value.movieCount = movieRes.value.data || 0
    } else {
      console.error('获取热映电影数量失败:', movieRes.reason)
      stats.value.movieCount = 0
    }
    
    if (cinemaRes.status === 'fulfilled') {
      stats.value.cinemaCount = cinemaRes.value.data || 0
    } else {
      console.error('获取营业影院数量失败:', cinemaRes.reason)
      stats.value.cinemaCount = 0
    }
    
    if (scheduleRes.status === 'fulfilled') {
      stats.value.scheduleCount = scheduleRes.value.data || 0
    } else {
      console.error('获取今日排片数量失败:', scheduleRes.reason)
      stats.value.scheduleCount = 0
    }
    
    if (orderRes.status === 'fulfilled') {
      stats.value.orderCount = orderRes.value.data || 0
    } else {
      console.error('获取今日订单数量失败:', orderRes.reason)
      stats.value.orderCount = 0
    }
    
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

const loadChartData = async () => {
  const todayDate = getTodayDate()
  
  try {
    const [movieRes, hallRes, orderStatusRes] = await Promise.allSettled([
      getMovieSalesStats(todayDate),
      getHallSalesStats(todayDate),
      getOrderStatusStats(todayDate)
    ])
    
    if (movieRes.status === 'fulfilled' && movieRes.value.data) {
      await nextTick()
      renderMovieChart(movieRes.value.data)
    } else {
      console.error('获取电影销售统计失败:', movieRes.reason)
    }
    
    if (hallRes.status === 'fulfilled' && hallRes.value.data) {
      await nextTick()
      renderHallChart(hallRes.value.data)
    } else {
      console.error('获取影厅销售统计失败:', hallRes.reason)
    }
    
    if (orderStatusRes.status === 'fulfilled' && orderStatusRes.value.data) {
      await nextTick()
      renderOrderStatusChart(orderStatusRes.value.data)
    } else {
      console.error('获取订单状态统计失败:', orderStatusRes.reason)
    }
    
  } catch (error) {
    console.error('加载图表数据失败:', error)
    ElMessage.error('加载图表数据失败')
  }
}

const renderMovieChart = (data) => {
  if (!movieChartRef.value) {
    console.error('电影图表容器未找到')
    return
  }
  
  console.log('开始渲染电影图表，数据:', data)
  
  if (!data || !Array.isArray(data) || data.length === 0) {
    console.warn('电影销售数据为空')
    ElMessage.warning('今日暂无电影销售数据')
    return
  }
  
  if (movieChart) {
    movieChart.dispose()
  }
  
  movieChart = echarts.init(movieChartRef.value)
  
  const seriesData = data.map(item => ({
    name: item.movieName,
    value: (item.totalAmount || 0) / 100
  }))
  
  console.log('处理后的系列数据:', seriesData)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4'],
    series: [
      {
        name: '销售额',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {d}%'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: true
        },
        data: seriesData
      }
    ]
  }
  
  movieChart.setOption(option)
  
  setTimeout(() => {
    movieChart?.resize()
  }, 100)
}

const renderHallChart = (data) => {
  if (!hallChartRef.value) {
    console.error('影厅图表容器未找到')
    return
  }
  
  console.log('开始渲染影厅图表，数据:', data)
  
  if (!data || !Array.isArray(data) || data.length === 0) {
    console.warn('影厅销售数据为空')
    ElMessage.warning('今日暂无影厅销售数据')
    return
  }
  
  if (hallChart) {
    hallChart.dispose()
  }
  
  hallChart = echarts.init(hallChartRef.value)
  
  const seriesData = data.map(item => ({
    name: item.hallName || item.cinemaName,
    value: (item.totalAmount || item.sales || 0) / 100
  }))
  
  console.log('处理后的系列数据:', seriesData)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    color: ['#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#5470c6', '#91cc75', '#fac858', '#ee6666'],
    series: [
      {
        name: '销售额',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {d}%'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: true
        },
        data: seriesData
      }
    ]
  }
  
  hallChart.setOption(option)
  
  setTimeout(() => {
    hallChart?.resize()
  }, 100)
}

const renderOrderStatusChart = (data) => {
  if (!orderStatusChartRef.value) {
    console.error('订单状态图表容器未找到')
    return
  }
  
  console.log('开始渲染订单状态图表，数据:', data)
  
  if (!data || !Array.isArray(data) || data.length === 0) {
    console.warn('订单状态数据为空')
    ElMessage.warning('今日暂无订单数据')
    return
  }
  
  if (orderStatusChart) {
    orderStatusChart.dispose()
  }
  
  orderStatusChart = echarts.init(orderStatusChartRef.value)
  
  const statusColorMap = {
    '待支付': '#F56C6C',
    '已支付': '#E6A23C',
    '已完成': '#67C23A',
    '已过期': '#909399',
    '已退款': '#409EFF',
    '已取消': '#909399'
  }
  
  const seriesData = data.map(item => ({
    name: item.statusText,
    value: item.orderCount || 0,
    itemStyle: {
      color: statusColorMap[item.statusText] || undefined
    }
  }))
  
  console.log('处理后的系列数据:', seriesData)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}单 ({d}%)'
    },
    legend: {
      data: data.map(item => item.statusText),
      bottom: 10
    },
    series: [
      {
        name: '订单状态',
        type: 'pie',
        radius: '70%',
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{c}单\n{d}%'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: true,
          length: 20,
          length2: 100
        },
        data: seriesData
      }
    ]
  }
  
  orderStatusChart.setOption(option)
  
  setTimeout(() => {
    orderStatusChart?.resize()
  }, 100)
}

const handleResize = () => {
  movieChart?.resize()
  hallChart?.resize()
  orderStatusChart?.resize()
}

onMounted(() => {
  loadStats()
  nextTick(() => {
    setTimeout(() => {
      loadChartData()
    }, 100)
  })
  window.addEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 15px;
}

.stat-icon .el-icon {
  font-size: 30px;
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 5px;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 400px;
  width: 100%;
}

.chart-container-large {
  height: 450px;
  width: 100%;
}

.welcome-card {
  margin-top: 20px;
}

.welcome-card h3 {
  margin: 0 0 15px 0;
  color: #333;
}

.welcome-card p {
  margin: 8px 0;
  color: #666;
}
</style>