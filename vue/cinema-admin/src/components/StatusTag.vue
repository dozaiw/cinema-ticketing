<template>
  <el-tag :type="tagType" :size="size" :effect="effect">
    {{ displayText }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 状态值
  status: {
    type: [Number, String],
    required: true
  },
  // 状态映射配置
  statusMap: {
    type: Object,
    default: () => ({})
  },
  // 尺寸
  size: {
    type: String,
    default: 'default',
    validator: (val) => ['large', 'default', 'small'].includes(val)
  },
  // 效果
  effect: {
    type: String,
    default: 'light',
    validator: (val) => ['dark', 'light', 'plain'].includes(val)
  }
})

// 默认状态映射（可根据业务修改）
const defaultStatusMap = {
  // 电影状态
  0: { text: '已下映', type: 'info' },
  1: { text: '热映中', type: 'success' },
  2: { text: '待上映', type: 'warning' },
  
  // 影院状态
  0: { text: '已停运', type: 'danger' },
  1: { text: '营业中', type: 'success' },
  
  // 订单状态
  'PENDING': { text: '待支付', type: 'warning' },
  'PAID': { text: '已支付', type: 'success' },
  'CANCELED': { text: '已取消', type: 'info' },
  'REFUNDED': { text: '已退款', type: 'danger' },
  
  // 座位状态
  0: { text: '可选', type: 'info' },
  1: { text: '已售', type: 'danger' },
  2: { text: '锁定', type: 'warning' }
}

const currentMap = computed(() => {
  return Object.keys(props.statusMap).length > 0 
    ? props.statusMap 
    : defaultStatusMap
})

const statusConfig = computed(() => {
  return currentMap.value[props.status] || { text: '未知', type: 'info' }
})

const tagType = computed(() => statusConfig.value.type)
const displayText = computed(() => statusConfig.value.text)
</script>

<style scoped>
/* 可自定义标签样式 */
</style>