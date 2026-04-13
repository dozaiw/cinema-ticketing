import { useUserStore } from '@/store/user'

/**
 * 权限指令
 * 使用方式：v-permission="['admin']" 或 v-permission="['admin', 'manager']"
 */
export default {
  mounted(el, binding) {
    const { value } = binding
    const userStore = useUserStore()
    const roles = userStore.roles || []
    
    if (value && value instanceof Array && value.length > 0) {
      const hasPermission = roles.some(role => value.includes(role))
      
      if (!hasPermission) {
        // 没有权限，移除元素
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error(`need roles! Like v-permission="['admin','editor']"`)
    }
  }
}