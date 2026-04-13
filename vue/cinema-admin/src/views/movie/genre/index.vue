<template>
  <div class="genre-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>🎬 电影类型管理</span>
          <el-button type="success" @click="handleAdd">+ 新增类型</el-button>
        </div>
      </template>
      
      <el-table :data="genreList" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="类型名称" min-width="150" />
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑类型' : '新增类型'"
      width="400px"
    >
      <el-form ref="genreFormRef" :model="genreForm" :rules="rules" label-width="80px">
        <el-form-item label="类型名称" prop="name">
          <el-input v-model="genreForm.name" placeholder="请输入类型名称" />
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
import { getGenreList, addGenre, updateGenre, deleteGenre } from '@/api/genre'

const genreList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const genreFormRef = ref(null)

const genreForm = reactive({
  id: null,
  name: ''
})

const rules = {
  name: [
    { required: true, message: '请输入类型名称', trigger: 'blur' },
    { min: 1, max: 20, message: '长度在 1 到 20 个字符', trigger: 'blur' }
  ]
}

const fetchGenreList = async () => {
  loading.value = true
  try {
    const res = await getGenreList()
    genreList.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(genreForm, { id: null, name: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(genreForm, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除 "${row.name}" 吗？`, '警告', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteGenre(row.id)
      ElMessage.success('删除成功')
      fetchGenreList()
    } catch (error) {
      console.error(error)
    }
  })
}

const handleSubmit = async () => {
  await genreFormRef.value?.validate()
  
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateGenre(genreForm)
      ElMessage.success('更新成功')
    } else {
      await addGenre(genreForm)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchGenreList()
  } catch (error) {
    console.error(error)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchGenreList()
})
</script>

<style scoped>
.genre-manage {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>