import permission from './permission'

/**
 * 全局注册自定义指令
 */
export default {
  install(app) {
    // 注册权限指令
    app.directive('permission', permission)
  }
}