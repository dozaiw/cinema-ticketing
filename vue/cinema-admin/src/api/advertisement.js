// src/api/advertisement.js
import request from '@/utils/request'

/**
 * 广告管理模块 API 封装
 * 基于 Apipost 文档: http://127.0.0.1:8001/advertisement/*
 */

// ============ 管理员接口 ============

/**
 * 分页查询广告列表（管理员）
 * @param {Object} params 
 * @param {number} params.pageNum - 页码，默认1
 * @param {number} params.pageSize - 每页数量，默认10
 * @param {string} params.title - 广告标题（模糊查询，可选）
 * @param {number} params.status - 状态：0-禁用 1-启用（可选）
 * @param {number} params.isDeleted - 是否删除：0-未删除 1-已删除（可选）
 */
export const getAdvertisementPage = (params) => {
  return request({
    url: '/advertisement/page',
    method: 'get',
    params: {
      pageNum: 1,
      pageSize: 10,
      ...params
    }
  })
}

/**
 * 新增广告（multipart/form-data）
 * @param {Object} adData - 广告信息对象
 * @param {File} imageFile - 广告图片文件
 */
export const addAdvertisement = (adData, imageFile) => {
  const formData = new FormData()
  
  // advertisement 字段必须是 JSON 字符串
  formData.append('advertisement', JSON.stringify({
    title: adData.title,
    linkUrl: adData.linkUrl || '',
    movieId: adData.movieId || null,
    status: adData.status ?? 1,
    sortOrder: adData.sortOrder ?? 0,
    isDeleted: 0  // 新增时固定为 0
  }))
  
  // 图片文件
  if (imageFile) {
    formData.append('imageFile', imageFile)
  }
  
  return request({
    url: '/advertisement/add',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 更新广告（multipart/form-data）
 * @param {Object} adData - 广告信息对象（必须包含 id）
 * @param {File|null} imageFile - 新图片文件（可选，不传则保留原图）
 * @param {string|null} oldImageUrl - 原图片 URL（编辑时保留原图需传）
 */
export const updateAdvertisement = (adData, imageFile = null, oldImageUrl = null) => {
  const formData = new FormData()
  
  formData.append('advertisement', JSON.stringify({
    id: adData.id,
    title: adData.title,
    linkUrl: adData.linkUrl || '',
    movieId: adData.movieId || null,
    status: adData.status ?? 1,
    sortOrder: adData.sortOrder ?? 0,
    isDeleted: adData.isDeleted ?? 0
  }))
  
  // 新图片
  if (imageFile) {
    formData.append('imageFile', imageFile)
  }
  
  // 保留原图时传 oldImageUrl
  if (oldImageUrl && !imageFile) {
    formData.append('oldImageUrl', oldImageUrl)
  }
  
  return request({
    url: '/advertisement/update',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 逻辑删除广告（is_deleted=1）
 * @param {number} id - 广告 ID
 */
export const deleteAdvertisement = (id) => {
  return request({
    url: `/advertisement/delete/${id}`,
    method: 'delete'
  })
}

/**
 * 恢复已删除广告（is_deleted=0）
 * @param {number} id - 广告 ID
 */
export const restoreAdvertisement = (id) => {
  return request({
    url: '/advertisement/update',
    method: 'post',
    data: {
      id,
      isDeleted: 0
    }
  })
}

/**
 * 切换广告状态（启用/禁用）
 * @param {number} id - 广告 ID
 * @param {number} status - 目标状态：0-禁用 1-启用
 */
export const toggleAdvertisementStatus = (id, status) => {
  return request({
    url: '/advertisement/update',
    method: 'post',
    data: {
      id,
      status
    }
  })
}

// ============ 用户端接口 ============

/**
 * 查询所有启用的广告（用户端，用于首页轮播）
 */
export const getEnabledAdvertisements = () => {
  return request({
    url: '/advertisement/list/enabled',
    method: 'get'
  })
}

/**
 * 根据 ID 查询广告详情
 * @param {number} id - 广告 ID
 */
export const getAdvertisementById = (id) => {
  return request({
    url: `/advertisement/${id}`,
    method: 'get'
  })
}