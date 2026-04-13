<template>
  <div class="cinema-list">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="影院名称">
          <el-input 
            v-model="searchForm.name" 
            placeholder="请输入影院名称" 
            clearable 
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        
        <!-- 省份选择 -->
        <el-form-item label="省份">
          <el-select 
            v-model="searchForm.province" 
            placeholder="选择省份" 
            clearable
            filterable
            style="width: 120px"
            @change="handleProvinceChange"
          >
            <el-option
              v-for="item in provinceOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        
        <!-- 城市选择（根据省份动态加载） -->
        <el-form-item label="城市">
          <el-select 
            v-model="searchForm.city" 
            placeholder="选择城市" 
            clearable
            filterable
            style="width: 150px"
            :disabled="!searchForm.province"
          >
            <el-option
              v-for="item in currentCityOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态">
          <el-select 
            v-model="searchForm.status" 
            placeholder="全部" 
            clearable
            style="width: 120px"
          >
            <el-option label="营业中" :value="1" />
            <el-option label="已停运" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">+ 新增影院</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card">
      <el-table 
        :data="cinemaList" 
        border 
        stripe 
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="影院名称" min-width="150" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <!-- <el-table-column prop="province" label="省份" width="100" /> -->
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="district" label="区域" width="100" />
        <el-table-column prop="phone" label="电话" width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '营业中' : '已停运' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="350" fixed="right" align="center">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="primary" 
              link 
              @click="showMap(row)"
            >
              🗺️ 地图
            </el-button>
            <el-button 
              size="small" 
              type="primary" 
              link 
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button 
              size="small" 
              :type="row.status === 1 ? 'warning' : 'success'" 
              link
              @click="handleChangeStatus(row)"
            >
              {{ row.status === 1 ? '停运' : '营业' }}
            </el-button>
            <el-button 
              size="small" 
              type="danger" 
              link 
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑影院' : '新增影院'"
      width="700px"
      @closed="handleDialogClose"
    >
      <el-form 
        ref="cinemaFormRef" 
        :model="cinemaForm" 
        :rules="rules" 
        label-width="100px"
      >
        <el-form-item label="影院名称" prop="name">
          <el-input v-model="cinemaForm.name" placeholder="请输入影院名称" />
        </el-form-item>
        
        <!-- ✅ 省份选择 -->
        <el-form-item label="省份" prop="province">
          <el-select 
            v-model="cinemaForm.province" 
            placeholder="选择省份" 
            filterable
            style="width: 100%"
            @change="handleProvinceChange"
          >
            <el-option
              v-for="item in provinceOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        
        <!-- ✅ 城市选择（根据省份动态加载） -->
        <el-form-item label="城市" prop="city">
          <el-select 
            v-model="cinemaForm.city" 
            placeholder="请先选择省份" 
            filterable
            style="width: 100%"
            :disabled="!cinemaForm.province"
          >
            <el-option
              v-for="item in currentCityOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="区域" prop="district">
          <el-input v-model="cinemaForm.district" placeholder="如：海淀区" />
        </el-form-item>
        
        <el-form-item label="详细地址" prop="address">
          <el-input 
            v-model="cinemaForm.address" 
            type="textarea" 
            :rows="2" 
            placeholder="请输入详细地址" 
          />
        </el-form-item>
        
        <el-form-item label="地图选点">
          <el-row :gutter="10">
            <el-col :span="18">
              <el-input 
                v-model="cinemaForm.address" 
                placeholder="输入地址后点击获取经纬度"
                @keyup.enter="handleGeocode"
              >
                <template #append>
                  <el-button @click="handleGeocode" :loading="geocodeLoading">
                    🗺️ 地址解析
                  </el-button>
                </template>
              </el-input>
            </el-col>
            <el-col :span="6">
              <el-button type="primary" @click="openMapPicker" style="width: 100%">
                📍 地图选点
              </el-button>
            </el-col>
          </el-row>
          <div class="coord-display" v-if="cinemaForm.latitude && cinemaForm.longitude">
            <el-tag size="small" type="info">
              纬度：{{ cinemaForm.latitude }} | 经度：{{ cinemaForm.longitude }}
            </el-tag>
          </div>
        </el-form-item>
        
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="cinemaForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 地图查看对话框 -->
    <el-dialog
      v-model="mapDialogVisible"
      :title="currentCinema?.name || '影院位置'"
      width="900px"
      :close-on-click-modal="false"
    >
      <!-- ✅ 改用原生 div 容器 -->
      <div ref="mapContainer" class="map-container"></div>
      
      <div class="map-info">
        <p><strong>📍 地址：</strong>{{ currentCinema?.address }}</p>
        <p><strong>📞 电话：</strong>{{ currentCinema?.phone }}</p>
        <p><strong>🌍 坐标：</strong>{{ currentCinema?.latitude }}, {{ currentCinema?.longitude }}</p>
      </div>
      
      <template #footer>
        <el-button @click="mapDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="openNavigation">
          🧭 导航到这里
        </el-button>
      </template>
    </el-dialog>

    <!-- 地图选点对话框 -->
    <el-dialog
      v-model="pickerDialogVisible"
      title="地图选点"
      width="900px"
      :close-on-click-modal="false"
    >
      <!-- ✅ 改用原生 div 容器 -->
      <div ref="pickerContainer" class="map-container" style="height: 500px"></div>
      
      <div class="picker-info" v-if="selectedPosition">
        <p><strong>选中位置：</strong>{{ selectedPosition[1] }}, {{ selectedPosition[0] }}</p>
        <p><strong>地址：</strong>{{ pickedAddress || '点击地图获取地址' }}</p>
      </div>
      
      <template #footer>
        <el-button @click="pickerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPicker" :disabled="!selectedPosition">
          确定选择
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, onBeforeUnmount, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getCinemaList, 
  addCinema, 
  updateCinema, 
  deleteCinema, 
  changeCinemaStatus,
  geocodeAddress 
} from '@/api/cinema'

// ✅ 中国省份列表
const provinceOptions = ref([
  { value: '北京', label: '北京' },
  { value: '天津', label: '天津' },
  { value: '上海', label: '上海' },
  { value: '重庆', label: '重庆' },
  { value: '河北', label: '河北' },
  { value: '山西', label: '山西' },
  { value: '辽宁', label: '辽宁' },
  { value: '吉林', label: '吉林' },
  { value: '黑龙江', label: '黑龙江' },
  { value: '江苏', label: '江苏' },
  { value: '浙江', label: '浙江' },
  { value: '安徽', label: '安徽' },
  { value: '福建', label: '福建' },
  { value: '江西', label: '江西' },
  { value: '山东', label: '山东' },
  { value: '河南', label: '河南' },
  { value: '湖北', label: '湖北' },
  { value: '湖南', label: '湖南' },
  { value: '广东', label: '广东' },
  { value: '海南', label: '海南' },
  { value: '四川', label: '四川' },
  { value: '贵州', label: '贵州' },
  { value: '云南', label: '云南' },
  { value: '陕西', label: '陕西' },
  { value: '甘肃', label: '甘肃' },
  { value: '青海', label: '青海' },
  { value: '台湾', label: '台湾' },
  { value: '内蒙古', label: '内蒙古' },
  { value: '广西', label: '广西' },
  { value: '西藏', label: '西藏' },
  { value: '宁夏', label: '宁夏' },
  { value: '新疆', label: '新疆' },
  { value: '香港', label: '香港' },
  { value: '澳门', label: '澳门' }
])

// ✅ 省份 - 城市映射关系
const provinceCityMap = {
  '北京': ['北京'],
  '天津': ['天津'],
  '上海': ['上海'],
  '重庆': ['重庆'],
  '河北': ['石家庄', '唐山', '保定', '邯郸', '沧州', '廊坊', '邢台', '衡水', '张家口', '承德', '秦皇岛'],
  '山西': ['太原', '大同', '阳泉', '长治', '晋城', '朔州', '晋中', '运城', '忻州', '临汾', '吕梁'],
  '辽宁': ['沈阳', '大连', '鞍山', '抚顺', '本溪', '丹东', '锦州', '营口', '阜新', '辽阳', '盘锦', '铁岭', '朝阳', '葫芦岛'],
  '吉林': ['长春', '吉林', '四平', '辽源', '通化', '白山', '松原', '白城', '延边'],
  '黑龙江': ['哈尔滨', '齐齐哈尔', '鸡西', '鹤岗', '双鸭山', '大庆', '伊春', '佳木斯', '七台河', '牡丹江', '黑河', '绥化', '大兴安岭'],
  '江苏': ['南京', '苏州', '无锡', '常州', '徐州', '南通', '扬州', '盐城', '镇江', '泰州', '淮安', '连云港', '宿迁'],
  '浙江': ['杭州', '宁波', '温州', '嘉兴', '湖州', '绍兴', '金华', '衢州', '舟山', '台州', '丽水'],
  '安徽': ['合肥', '芜湖', '蚌埠', '淮南', '马鞍山', '淮北', '铜陵', '安庆', '黄山', '滁州', '阜阳', '宿州', '六安', '亳州', '池州', '宣城'],
  '福建': ['福州', '厦门', '泉州', '漳州', '莆田', '三明', '南平', '龙岩', '宁德'],
  '江西': ['南昌', '景德镇', '萍乡', '九江', '新余', '鹰潭', '赣州', '吉安', '宜春', '抚州', '上饶'],
  '山东': ['济南', '青岛', '烟台', '潍坊', '淄博', '威海', '济宁', '泰安', '临沂', '德州', '聊城', '滨州', '菏泽', '日照', '东营', '枣庄'],
  '河南': ['郑州', '洛阳', '开封', '安阳', '新乡', '许昌', '平顶山', '焦作', '南阳', '商丘', '信阳', '周口', '驻马店', '漯河', '三门峡', '濮阳', '鹤壁'],
  '湖北': ['武汉', '宜昌', '襄阳', '荆州', '十堰', '黄石', '黄冈', '孝感', '咸宁', '随州', '恩施', '鄂州'],
  '湖南': ['长沙', '株洲', '湘潭', '衡阳', '邵阳', '岳阳', '常德', '张家界', '益阳', '郴州', '永州', '怀化', '娄底', '湘西'],
  '广东': ['广州', '深圳', '珠海', '佛山', '东莞', '中山', '惠州', '江门', '肇庆', '汕头', '湛江', '茂名', '韶关', '清远', '揭阳', '潮州', '河源', '梅州', '汕尾', '阳江', '云浮'],
  '海南': ['海口', '三亚', '三沙', '儋州'],
  '四川': ['成都', '绵阳', '德阳', '宜宾', '南充', '乐山', '泸州', '达州', '内江', '自贡', '攀枝花', '遂宁', '广安', '眉山', '雅安', '资阳', '广元', '巴中'],
  '贵州': ['贵阳', '遵义', '六盘水', '安顺', '毕节', '铜仁', '黔西南', '黔东南', '黔南'],
  '云南': ['昆明', '曲靖', '玉溪', '保山', '昭通', '丽江', '普洱', '临沧', '楚雄', '红河', '文山', '西双版纳', '大理', '德宏', '怒江', '迪庆'],
  '陕西': ['西安', '咸阳', '宝鸡', '渭南', '铜川', '延安', '榆林', '汉中', '安康', '商洛'],
  '甘肃': ['兰州', '嘉峪关', '金昌', '白银', '天水', '武威', '张掖', '平凉', '酒泉', '庆阳', '定西', '陇南', '临夏', '甘南'],
  '青海': ['西宁', '海东', '海北', '黄南', '海南', '果洛', '玉树', '海西'],
  '台湾': ['台北', '高雄', '台中', '台南', '新北', '桃园', '新竹', '嘉义', '基隆', '彰化', '南投', '云林', '屏东', '宜兰', '花莲', '台东', '澎湖', '金门', '马祖'],
  '内蒙古': ['呼和浩特', '包头', '乌海', '赤峰', '通辽', '鄂尔多斯', '呼伦贝尔', '巴彦淖尔', '乌兰察布', '兴安', '锡林郭勒', '阿拉善'],
  '广西': ['南宁', '柳州', '桂林', '梧州', '北海', '防城港', '钦州', '贵港', '玉林', '百色', '贺州', '河池', '来宾', '崇左'],
  '西藏': ['拉萨', '日喀则', '昌都', '林芝', '山南', '那曲', '阿里'],
  '宁夏': ['银川', '石嘴山', '吴忠', '固原', '中卫'],
  '新疆': ['乌鲁木齐', '克拉玛依', '吐鲁番', '哈密', '昌吉', '博尔塔拉', '巴音郭楞', '阿克苏', '克孜勒苏', '喀什', '和田', '伊犁', '塔城', '阿勒泰'],
  '香港': ['香港'],
  '澳门': ['澳门']
}

// ✅ 根据选中省份动态获取城市列表
const currentCityOptions = computed(() => {
  const province = cinemaForm.province || searchForm.province
  if (!province) {
    return []
  }
  const cities = provinceCityMap[province] || []
  return cities.map(city => ({
    value: city,
    label: city
  }))
})

// 数据
const cinemaList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const geocodeLoading = ref(false)
const cinemaFormRef = ref(null)

// 地图相关（原生 AMap）
const mapDialogVisible = ref(false)
const pickerDialogVisible = ref(false)
const currentCinema = ref(null)
const mapContainer = ref(null)
const pickerContainer = ref(null)
let viewMap = null
let pickerMap = null
let marker = null
let pickerMarker = null
const selectedPosition = ref(null)
const pickedAddress = ref('')
const pickerCenter = ref([116.397428, 39.90923])

const searchForm = reactive({
  name: '',
  province: '',
  city: '',
  status: ''
})

const cinemaForm = reactive({
  id: null,
  name: '',
  address: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  latitude: null,
  longitude: null,
  status: 1
})

const rules = {
  name: [
    { required: true, message: '请输入影院名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  province: [
    { required: true, message: '请选择省份', trigger: 'change' }
  ],
  address: [
    { required: true, message: '请输入详细地址', trigger: 'blur' }
  ],
  city: [
    { required: true, message: '请选择城市', trigger: 'change' }
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^[\d\s\-\(\)]{7,20}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 获取影院列表
const fetchCinemaList = async () => {
  loading.value = true
  try {
    const res = await getCinemaList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: searchForm.name || undefined,
      province: searchForm.province || undefined,
      city: searchForm.city || undefined,
      status: searchForm.status !== '' && searchForm.status != null ? searchForm.status : undefined
    })
    cinemaList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error(error)
    ElMessage.error('获取影院列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  fetchCinemaList()
}

// 重置
const handleReset = () => {
  searchForm.name = ''
  searchForm.province = ''
  searchForm.city = ''
  searchForm.status = ''
  handleSearch()
}

// 省份变化时清空城市
const handleProvinceChange = () => {
  cinemaForm.city = ''
  searchForm.city = ''
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  Object.assign(cinemaForm, {
    id: null,
    name: '',
    address: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    latitude: null,
    longitude: null,
    status: 1
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(cinemaForm, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

// 地址解析
const handleGeocode = async () => {
  if (!cinemaForm.address) {
    ElMessage.warning('请先输入详细地址')
    return
  }
  
  geocodeLoading.value = true
  try {
    const res = await geocodeAddress(cinemaForm.address)
    cinemaForm.latitude = res.data.latitude
    cinemaForm.longitude = res.data.longitude
    cinemaForm.address = res.data.formattedAddress || cinemaForm.address
    ElMessage.success('位置获取成功')
  } catch (error) {
    ElMessage.error(error.message || '位置获取失败')
  } finally {
    geocodeLoading.value = false
  }
}

// 🗺️ 显示地图（原生 AMap）
const showMap = (row) => {
  if (!row.latitude || !row.longitude) {
    ElMessage.warning('该影院暂无位置信息')
    return
  }
  currentCinema.value = row
  mapDialogVisible.value = true
  
  nextTick(() => {
    initMapView()
  })
}

// 初始化查看地图
const initMapView = () => {
  if (!window.AMap || !mapContainer.value) {
    console.error('地图初始化失败：AMap 或容器未就绪')
    return
  }
  
  if (viewMap) {
    viewMap.destroy()
    viewMap = null
  }
  
  viewMap = new window.AMap.Map(mapContainer.value, {
    zoom: 15,
    center: [currentCinema.value.longitude, currentCinema.value.latitude],
    viewMode: '2D',
    resizeEnable: true
  })
  
  marker = new window.AMap.Marker({
    position: [currentCinema.value.longitude, currentCinema.value.latitude],
    map: viewMap,
    title: currentCinema.value.name
  })
}

// 打开地图选点
const openMapPicker = () => {
  selectedPosition.value = null
  pickedAddress.value = ''
  
  if (cinemaForm.latitude && cinemaForm.longitude) {
    pickerCenter.value = [cinemaForm.longitude, cinemaForm.latitude]
  }
  
  pickerDialogVisible.value = true
  
  nextTick(() => {
    initPickerMap()
  })
}

// 初始化选点地图
const initPickerMap = () => {
  if (!window.AMap || !pickerContainer.value) {
    console.error('选点地图初始化失败')
    return
  }
  
  if (pickerMap) {
    pickerMap.destroy()
    pickerMap = null
  }
  
  pickerMap = new window.AMap.Map(pickerContainer.value, {
    zoom: 15,
    center: pickerCenter.value,
    viewMode: '2D',
    resizeEnable: true
  })
  
  if (selectedPosition.value) {
    addPickerMarker(selectedPosition.value)
  }
  
  pickerMap.on('click', (e) => {
    selectedPosition.value = e.lnglat
    addPickerMarker(e.lnglat)
    reverseGeocode(e.lnglat)
  })
}

// 添加选点标记
const addPickerMarker = (position) => {
  if (pickerMarker) {
    pickerMarker.setMap(null)
  }
  
  pickerMarker = new window.AMap.Marker({
    position: position,
    map: pickerMap,
    draggable: true,
    title: '拖动调整位置'
  })
  
  pickerMarker.on('dragend', (e) => {
    selectedPosition.value = e.lnglat
    reverseGeocode(e.lnglat)
  })
}

// 逆地理编码（坐标转地址）
const reverseGeocode = (lnglat) => {
  if (!window.AMap) return
  
  const geocoder = new window.AMap.Geocoder({
    radius: 1000,
    extensions: 'all'
  })
  
  geocoder.getAddress(lnglat, (status, result) => {
    if (status === 'complete' && result.regeocode) {
      pickedAddress.value = result.regeocode.formattedAddress
    }
  })
}

// 确认选点
const confirmPicker = () => {
  if (!selectedPosition.value) return
  
  cinemaForm.longitude = selectedPosition.value[0]
  cinemaForm.latitude = selectedPosition.value[1]
  if (pickedAddress.value) {
    cinemaForm.address = pickedAddress.value
  }
  
  pickerDialogVisible.value = false
  ElMessage.success('位置选择成功')
}

// 打开导航
const openNavigation = () => {
  if (!currentCinema.value) return
  
  const url = `https://uri.amap.com/navigation?from=0,0&to=${currentCinema.value.longitude},${currentCinema.value.latitude}&mode=car&policy=1&coordinate=gaode`
  window.open(url, '_blank')
}

// 修改状态
const handleChangeStatus = async (row) => {
  const action = row.status === 1 ? '停运' : '恢复营业'
  await ElMessageBox.confirm(`确定要${action} "${row.name}" 吗？`, '提示', {
    type: 'warning'
  })
  
  try {
    await changeCinemaStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    fetchCinemaList()
  } catch (error) {
    console.error(error)
    ElMessage.error('操作失败')
  }
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除 "${row.name}" 吗？删除后无法恢复！`, '警告', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await deleteCinema(row.id)
      ElMessage.success('删除成功')
      fetchCinemaList()
    } catch (error) {
      console.error(error)
      ElMessage.error('删除失败')
    }
  })
}

// 提交
const handleSubmit = async () => {
  await cinemaFormRef.value?.validate()
  
  if (!cinemaForm.latitude || !cinemaForm.longitude) {
    ElMessage.warning('请先获取经纬度（地址解析或地图选点）')
    return
  }
  
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateCinema(cinemaForm)
      ElMessage.success('更新成功')
    } else {
      await addCinema(cinemaForm)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchCinemaList()
  } catch (error) {
    console.error(error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 对话框关闭
const handleDialogClose = () => {
  cinemaFormRef.value?.resetFields()
}

// 分页
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchCinemaList()
}

const handlePageChange = (val) => {
  pageNum.value = val
  fetchCinemaList()
}

// 组件卸载时清理地图实例
onBeforeUnmount(() => {
  if (viewMap) {
    viewMap.destroy()
    viewMap = null
  }
  if (pickerMap) {
    pickerMap.destroy()
    pickerMap = null
  }
})

onMounted(() => {
  fetchCinemaList()
})
</script>

<style scoped>
.cinema-list {
  padding: 20px;
}
.search-card, .table-card {
  margin-bottom: 20px;
}
.coord-display {
  margin-top: 8px;
}
.map-container {
  height: 450px;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
  background: #f5f7fa;
}
.map-info {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
}
.map-info p {
  margin: 6px 0;
}
.picker-info {
  margin-top: 12px;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 4px;
  font-size: 14px;
}
.picker-info p {
  margin: 4px 0;
}
</style>