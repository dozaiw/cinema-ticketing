<template>
  <div class="comment-report-page">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="处理状态">
          <el-select v-model="searchForm.reportStatus" placeholder="全部" clearable style="width: 160px">
            <el-option label="待处理" :value="0" />
            <el-option label="已驳回" :value="1" />
            <el-option label="评论已删除" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报原因">
          <el-select v-model="searchForm.reasonType" placeholder="全部" clearable style="width: 180px">
            <el-option
              v-for="item in reasonOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="举报ID" width="90" align="center" />
        <el-table-column label="电影" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.movieTitle || (row.movieId ? `电影ID：${row.movieId}` : '-') }}
          </template>
        </el-table-column>
        <el-table-column prop="reasonType" label="举报原因" width="120" align="center" />
        <el-table-column prop="reporterNickname" label="举报人" width="120" align="center" />
        <el-table-column prop="commentAuthorNickname" label="评论作者" width="120" align="center" />
        <el-table-column label="评论内容" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.commentContent || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="补充说明" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.reportContent || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.reportStatus)">
              {{ getStatusText(row.reportStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="举报时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.reportTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">
              {{ row.reportStatus === 0 ? '审核' : '查看' }}
            </el-button>
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

    <el-dialog v-model="detailVisible" title="评论举报详情" width="760px">
      <template v-if="currentReport">
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="举报ID">{{ currentReport.id }}</el-descriptions-item>
          <el-descriptions-item label="处理状态">
            <el-tag :type="getStatusTagType(currentReport.reportStatus)">
              {{ getStatusText(currentReport.reportStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="电影">{{ currentReport.movieTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="举报原因">{{ currentReport.reasonType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="举报人">{{ currentReport.reporterNickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="评论作者">{{ currentReport.commentAuthorNickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="举报时间">{{ formatDateTime(currentReport.reportTime) }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ formatDateTime(currentReport.handleTime) }}</el-descriptions-item>
          <el-descriptions-item label="评论状态">{{ getCommentStatusText(currentReport.commentStatus) }}</el-descriptions-item>
          <el-descriptions-item label="处理备注">{{ currentReport.adminRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="评论内容" :span="2">
            <div class="text-block">{{ currentReport.commentContent || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="补充说明" :span="2">
            <div class="text-block">{{ currentReport.reportContent || '-' }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <el-form v-if="currentReport.reportStatus === 0" label-width="90px" class="remark-form">
          <el-form-item label="审核备注">
            <el-input
              v-model="adminRemark"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              placeholder="可选，填写处理说明"
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentReport && currentReport.reportStatus === 0"
          :loading="submitting"
          @click="submitHandle(1)"
        >
          驳回举报
        </el-button>
        <el-button
          v-if="currentReport && currentReport.reportStatus === 0"
          type="danger"
          :loading="submitting"
          @click="submitHandle(2)"
        >
          删除评论
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCommentReportPage, handleCommentReport } from '@/api/comment'

const reasonOptions = ['辱骂攻击', '广告营销', '色情低俗', '虚假信息', '其他']

const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const currentReport = ref(null)
const adminRemark = ref('')
const submitting = ref(false)

const searchForm = reactive({
  reportStatus: 0,
  reasonType: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getCommentReportPage({
      reportStatus: searchForm.reportStatus,
      reasonType: searchForm.reasonType || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error('获取评论举报列表失败:', error)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchList()
}

const handleReset = () => {
  searchForm.reportStatus = 0
  searchForm.reasonType = ''
  pagination.pageNum = 1
  pagination.pageSize = 10
  fetchList()
}

const openDetail = (row) => {
  currentReport.value = { ...row }
  adminRemark.value = row.adminRemark || ''
  detailVisible.value = true
}

const submitHandle = async (action) => {
  if (!currentReport.value) return

  try {
    const actionText = action === 1 ? '驳回举报并保留评论' : '删除评论并处理举报'
    await ElMessageBox.confirm(`确定要${actionText}吗？`, '审核确认', {
      type: 'warning'
    })

    submitting.value = true
    await handleCommentReport({
      reportId: currentReport.value.id,
      action,
      adminRemark: adminRemark.value?.trim() || ''
    })
    ElMessage.success(action === 1 ? '已驳回举报' : '已删除评论')
    detailVisible.value = false
    await fetchList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('处理评论举报失败:', error)
    }
  } finally {
    submitting.value = false
  }
}

const getStatusText = (status) => {
  const map = {
    0: '待处理',
    1: '已驳回',
    2: '评论已删除'
  }
  return map[status] || '未知'
}

const getStatusTagType = (status) => {
  const map = {
    0: 'warning',
    1: 'info',
    2: 'danger'
  }
  return map[status] || 'info'
}

const getCommentStatusText = (status) => {
  const map = {
    0: '待审核',
    1: '正常',
    2: '已删除'
  }
  return map[status] || '-'
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.comment-report-page {
  padding: 20px;
}

.search-card,
.table-card {
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.detail-desc {
  margin-bottom: 20px;
}

.remark-form {
  margin-top: 20px;
}

.text-block {
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
