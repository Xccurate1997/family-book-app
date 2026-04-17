import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

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
