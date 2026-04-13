import UploadImage from './UploadImage.vue'
import UploadVideo from './UploadVideo.vue'
import Pagination from './Pagination.vue'
import StatusTag from './StatusTag.vue'
import SearchForm from './SearchForm.vue'

export default {
  install(app) {
    app.component('UploadImage', UploadImage)
    app.component('UploadVideo', UploadVideo)
    app.component('Pagination', Pagination)
    app.component('StatusTag', StatusTag)
    app.component('SearchForm', SearchForm)
  }
}