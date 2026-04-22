import axios from 'axios'
import { useAuth } from '../composables/useAuth.js'

const api = axios.create({ baseURL: '/api' })

// 请求拦截器：自动携带 JWT Token
api.interceptors.request.use((config) => {
  const { getToken } = useAuth()
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：401 时清除认证并跳转登录
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const { clearAuth } = useAuth()
      clearAuth()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// ── 认证 ──────────────────────────────────────────────
export const login = (username, password) =>
  api.post('/auth/login', { username, password })

export const register = (data) => api.post('/auth/register', data)

export const getCurrentUser = () => api.get('/auth/me')

// ── 账本 ──────────────────────────────────────────────
export const getLedgers = () => api.get('/ledgers')
export const createLedger = (data) => api.post('/ledgers', data)
export const updateLedger = (id, data) => api.put(`/ledgers/${id}`, data)
export const deleteLedger = (id) => api.delete(`/ledgers/${id}`)

// ── 分类 ──────────────────────────────────────────────
export const getCategories = () => api.get('/categories')
export const createCategory = (data) => api.post('/categories', data)
export const updateCategory = (id, data) => api.put(`/categories/${id}`, data)
export const deleteCategory = (id) => api.delete(`/categories/${id}`)

// ── 交易 ──────────────────────────────────────────────
export const getTransactions = (ledgerId, year, month, filters = {}) =>
  api.get('/transactions', { params: { ledgerId, year, month, ...filters } })

export const getSummary = (ledgerId, year, month) =>
  api.get('/transactions/summary', { params: { ledgerId, year, month } })

export const createTransaction = (data) => api.post('/transactions', data)
export const updateTransaction = (id, data) => api.put(`/transactions/${id}`, data)
export const deleteTransaction = (id) => api.delete(`/transactions/${id}`)

export const exportTransactions = (ledgerId, year, month) =>
  api.get('/transactions/export', {
    params: { ledgerId, year, month },
    responseType: 'blob'
  })

// ── 统计 ──────────────────────────────────────────────
export const getMonthlyTrend = (ledgerId, months = 6) =>
  api.get('/stats/monthly-trend', { params: { ledgerId, months } })

export const getCategoryBreakdown = (ledgerId, year, month) =>
  api.get('/stats/category-breakdown', { params: { ledgerId, year, month } })

export const getDailyStats = (ledgerId, year, month) =>
  api.get('/stats/daily', { params: { ledgerId, year, month } })

// ── 年度统计 ─────────────────────────────────────────
export const getYearlySummary = (ledgerId, year) =>
  api.get('/stats/yearly-summary', { params: { ledgerId, year } })

export const getYearlyMonthlyTrend = (ledgerId, year) =>
  api.get('/stats/yearly-monthly-trend', { params: { ledgerId, year } })

export const getYearlyCategoryRanking = (ledgerId, year, limit = 10) =>
  api.get('/stats/yearly-category-ranking', { params: { ledgerId, year, limit } })

// ── 按日期查询交易 ───────────────────────────────────
export const getTransactionsByDate = (ledgerId, date) =>
  api.get('/transactions/by-date', { params: { ledgerId, date } })

// ── 特效 ──────────────────────────────────────────────
export const markEffectPlayed = (id) => api.post(`/effects/${id}/played`)

// ── 主题（用户） ─────────────────────────────────────
export const getAvailableThemes = () => api.get('/themes/available')
export const getThemeDetail = (id) => api.get(`/themes/${id}`)

// ── 管理：主题 ────────────────────────────────────────
export const getAllThemes = () => api.get('/themes')
export const createTheme = (data) => api.post('/themes', data)
export const updateTheme = (id, data) => api.put(`/themes/${id}`, data)
export const deleteTheme = (id) => api.delete(`/themes/${id}`)
export const getThemeAssignments = (id) => api.get(`/themes/${id}/assignments`)
export const assignThemeToUser = (id, username) => api.post(`/themes/${id}/assignments`, { username })
export const removeThemeAssignment = (themeId, username) => api.delete(`/themes/${themeId}/assignments/${username}`)
export const uploadThemeImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/themes/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const uploadThemeSound = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/themes/upload-sound', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ── 管理：表情规则 ────────────────────────────────────
export const getEmoticonRules = () => api.get('/emoticon-rules')
export const createEmoticonRule = (data) => api.post('/emoticon-rules', data)
export const updateEmoticonRule = (id, data) => api.put(`/emoticon-rules/${id}`, data)
export const deleteEmoticonRule = (id) => api.delete(`/emoticon-rules/${id}`)
export const enableAllEmoticonRules = () => api.post('/emoticon-rules/enable-all')
export const disableAllEmoticonRules = () => api.post('/emoticon-rules/disable-all')
export const uploadEmoticonImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/emoticon-images/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ── 管理：彩蛋规则 ────────────────────────────────────
export const getEffectRules = () => api.get('/effect-rules')
export const createEffectRule = (data) => api.post('/effect-rules', data)
export const updateEffectRule = (id, data) => api.put(`/effect-rules/${id}`, data)
export const deleteEffectRule = (id) => api.delete(`/effect-rules/${id}`)
export const uploadEffectVideo = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/effect-videos/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
