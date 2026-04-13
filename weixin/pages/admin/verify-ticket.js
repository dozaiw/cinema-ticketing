const { request, BASE_URL } = require('../../utils/request')
const { getUserInfo, isAdmin } = require('../../utils/auth')

Page({
  data: {
    verifyCode: '',
    verifying: false,
    verifyResult: null,
    lastVerifyTime: ''
  },

  onLoad() {
    this.ensureAdminAccess()
  },

  onShow() {
    this.ensureAdminAccess()
  },

  ensureAdminAccess() {
    const userInfo = getUserInfo()
    if (!isAdmin(userInfo)) {
      wx.showToast({
        title: '仅管理员可使用',
        icon: 'none',
        duration: 1800
      })

      setTimeout(() => {
        wx.switchTab({ url: '/pages/user/user' })
      }, 1500)
      return false
    }

    return true
  },

  onVerifyCodeInput(e) {
    this.setData({
      verifyCode: (e.detail.value || '').trim()
    })
  },

  async handleVerify() {
    if (!this.ensureAdminAccess()) {
      return
    }

    const verifyCode = (this.data.verifyCode || '').trim()
    if (!verifyCode) {
      wx.showToast({ title: '请输入验票码', icon: 'none' })
      return
    }

    await this.verifyTicket(verifyCode)
  },

  handleScanVerify() {
    if (!this.ensureAdminAccess()) {
      return
    }

    wx.scanCode({
      onlyFromCamera: false,
      scanType: ['qrCode', 'barCode'],
      success: async res => {
        const verifyCode = this.extractVerifyCode(res.result)
        if (!verifyCode) {
          wx.showToast({ title: '未识别到验票码', icon: 'none' })
          return
        }

        this.setData({ verifyCode })
        await this.verifyTicket(verifyCode)
      },
      fail: err => {
        if (err && err.errMsg && err.errMsg.includes('cancel')) {
          return
        }
        wx.showToast({ title: '扫码失败', icon: 'none' })
      }
    })
  },

  extractVerifyCode(rawResult) {
    if (!rawResult) {
      return ''
    }

    const text = String(rawResult).trim()
    if (!text) {
      return ''
    }

    if (text.startsWith('TICKET_')) {
      return text
    }

    const parts = text.split('|').map(item => item.trim()).filter(Boolean)
    const lastPart = parts[parts.length - 1] || ''
    if (lastPart.startsWith('TICKET_')) {
      return lastPart
    }

    return ''
  },

  async verifyTicket(verifyCode) {
    this.setData({ verifying: true })
    wx.showLoading({ title: '验票中...', mask: true })

    try {
      const res = await request({
        url: `${BASE_URL.ORDER}/order/verifyTicket`,
        method: 'POST',
        data: { verifyCode },
        header: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      })

      wx.hideLoading()

      const result = res.data || {}
      this.setData({
        verifyResult: {
          orderNo: result.orderNo || '',
          movieName: result.movieName || '',
          hallName: result.hallName || '',
          cinemaName: result.cinemaName || '',
          showTime: this.formatTime(result.showTime),
          seatInfo: this.formatSeatInfo(result.seatInfo)
        },
        lastVerifyTime: this.formatTime(new Date()),
        verifyCode: ''
      })

      wx.showToast({ title: '验票成功', icon: 'success' })
    } catch (err) {
      wx.hideLoading()
      wx.showToast({
        title: err?.msg || err?.message || '验票失败',
        icon: 'none',
        duration: 2000
      })
    } finally {
      this.setData({ verifying: false })
    }
  },

  formatSeatInfo(seatInfo) {
    if (!seatInfo) {
      return ''
    }

    if (Array.isArray(seatInfo)) {
      return seatInfo.join('、')
    }

    if (typeof seatInfo === 'string') {
      try {
        const parsed = JSON.parse(seatInfo)
        return Array.isArray(parsed) ? parsed.join('、') : seatInfo
      } catch (error) {
        return seatInfo
      }
    }

    return String(seatInfo)
  },

  formatTime(time) {
    if (!time) {
      return ''
    }

    if (time instanceof Date) {
      const year = time.getFullYear()
      const month = String(time.getMonth() + 1).padStart(2, '0')
      const day = String(time.getDate()).padStart(2, '0')
      const hour = String(time.getHours()).padStart(2, '0')
      const minute = String(time.getMinutes()).padStart(2, '0')
      const second = String(time.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hour}:${minute}:${second}`
    }

    return String(time).replace('T', ' ')
  },

  resetResult() {
    this.setData({
      verifyCode: '',
      verifyResult: null,
      lastVerifyTime: ''
    })
  }
})
