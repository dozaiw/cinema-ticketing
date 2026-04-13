<template>
  <el-menu
    :default-active="activeMenu"
    class="el-menu-vertical"
    background-color="#304156"
    text-color="#bfcbd9"
    active-text-color="#409EFF"
    router
    :collapse="isCollapse"
  >
    <div class="logo">
      <span v-if="!isCollapse">🎬 影院管理</span>
      <span v-else>🎬</span>
    </div>
    
    <!-- 首页 -->
    <el-menu-item index="/dashboard">
      <el-icon><Odometer /></el-icon>
      <template #title>首页</template>
    </el-menu-item>
    
    <!-- 影院管理 -->
    <el-sub-menu index="/cinema">
      <template #title>
        <el-icon><OfficeBuilding /></el-icon>
        <span>影院管理</span>
      </template>
      <el-menu-item index="/cinema/list">影院列表</el-menu-item>
      <el-menu-item index="/cinema/hall">影厅管理</el-menu-item>
    </el-sub-menu>
    
    <!-- 电影管理 -->
    <el-sub-menu index="/movie">
      <template #title>
        <el-icon><Film /></el-icon>
        <span>电影管理</span>
      </template>
      <el-menu-item index="/movie/list">电影列表</el-menu-item>
      <el-menu-item index="/movie/genre">类型管理</el-menu-item>
      <el-menu-item index="/movie/actor">演员管理</el-menu-item>
      <el-menu-item index="/movie/comment">评论审核</el-menu-item>
    </el-sub-menu>
    
    <!-- 排片管理 -->
    <el-menu-item index="/schedule">
      <el-icon><Calendar /></el-icon>
      <template #title>排片管理</template>
    </el-menu-item>
    
    <!-- 订单管理 -->
    <el-menu-item index="/order">
      <el-icon><Document /></el-icon>
      <template #title>订单管理</template>
    </el-menu-item>
    
    <!-- 用户管理 - 权限控制 -->
    <el-menu-item index="/user" v-if="checkPermission(['admin'])">
      <el-icon><User /></el-icon>
      <template #title>用户管理</template>
    </el-menu-item>

    <el-menu-item index="/advertisement" v-if="checkPermission(['admin'])">
      <el-icon><User /></el-icon>
      <template #title>广告管理</template>
    </el-menu-item>

  </el-menu>

</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ref } from 'vue'

const route = useRoute()
const userStore = useUserStore()
const isCollapse = ref(false)

// ✅ 高亮当前激活菜单（处理嵌套路由）
const activeMenu = computed(() => {
  const { path, meta } = route
  // 如果路由有 activeMenu 配置则优先使用
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})

// ✅ 权限检查函数（替代 v-permission 指令，更简单直接）
const checkPermission = (roles) => {
  if (!roles || roles.length === 0) return true
  const userRole = userStore.role || 'admin' // 根据实际情况调整
  return roles.includes(userRole)
}
</script>

<style scoped>
.el-menu-vertical {
  height: 100%;
  border-right: none;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background-color: #2b3a4b;
}

/* 折叠动画优化 */
.el-menu-vertical:not(.el-menu--collapse) {
  width: 200px;
}
</style>
