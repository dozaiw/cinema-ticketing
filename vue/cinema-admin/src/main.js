// main.js
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './store'
import components from './components'
import directive from './directive'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

// ✅ 导入高德官方加载器
import AMapLoader from '@amap/amap-jsapi-loader'

const app = createApp(App)

// ✅ 异步加载高德地图
AMapLoader.load({
  key: 'f65ec319f7189ed25177f16e114b940e',  // 你的高德 Key
  version: '2.0',                              // 高德 JS API 版本
  plugins: [                                   // 需要的插件
    'AMap.Scale',
    'AMap.OverView',
    'AMap.ToolBar',
    'AMap.Marker',
    'AMap.Geocoder',
    'AMap.PlaceSearch'
  ]
}).then((AMap) => {
  // ✅ 将 AMap 挂载到全局，供组件使用
  window.AMap = AMap
  
  // ✅ 地图加载完成后再注册其他插件和挂载应用
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  app.use(pinia)
  app.use(router)
  app.use(ElementPlus, { locale: zhCn })
  app.use(components)
  app.use(directive)
  app.mount('#app')
  
}).catch((error) => {
  console.error('❌ 高德地图加载失败:', error)
})