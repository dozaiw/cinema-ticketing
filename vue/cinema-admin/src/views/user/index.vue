<template>
  <div class="user-manage">
    <el-card class="search-card" style="margin-bottom:20px">
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="昵称">
        <el-input v-model="searchForm.nickname" placeholder="请输入昵称" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="searchForm.role" placeholder="全部" clearable style="width:120px">
          <el-option label="管理员" :value="0" />
          <el-option label="普通用户" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>

  <el-card>
      <template #header>
        <div class="card-header">
          <span>👥 用户管理</span>
        </div>
      </template>
      
      <el-table :data="userList" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="用户名" min-width="150" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="role" label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 0 ? 'danger' : 'primary'">
              {{ row.role === 0 ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.status === 1 ? 'danger' : 'success'"
              link
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '封禁' : '解封' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFilteredUserList, changeUserState } from '@/api/auth'

const userList = ref([])
const loading = ref(false)

const searchForm = reactive({
  nickname: '',
  status: '',
  role: ''
})

const fetchUserList = async (params = {}) => {
  loading.value = true
  try {
    // 使用后端过滤接口
    const res = await getFilteredUserList(params)
    userList.value = res.data.records || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  fetchUserList({
    nickname: searchForm.nickname || undefined,
    status: searchForm.status !== '' ? searchForm.status : undefined,
    role: searchForm.role !== '' ? searchForm.role : undefined
  })
}

const handleReset = () => {
  searchForm.nickname = ''
  searchForm.status = ''
  searchForm.role = ''
  fetchUserList()
}

const toggleStatus = (row) => {
  const targetState = row.status === 1 ? 0 : 1
  const action = row.status === 1 ? '封禁' : '解封'
  ElMessageBox.confirm(`确认要${action}用户 "${row.username}" 吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await changeUserState(row.username, targetState)
      ElMessage.success(`${action}成功`)
      fetchUserList()
    } catch (err) {
      console.error(err)
      ElMessage.error(`${action}失败`)
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchUserList()
})
</script>

<style scoped>
.user-manage {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>