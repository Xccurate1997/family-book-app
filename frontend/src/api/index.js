import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

export const getCategories = () =>
  api.get('/categories')

export const getTransactions = (year, month) =>
  api.get('/transactions', { params: { year, month } })

export const getSummary = (year, month) =>
  api.get('/transactions/summary', { params: { year, month } })

export const createTransaction = (data) =>
  api.post('/transactions', data)

export const updateTransaction = (id, data) =>
  api.put(`/transactions/${id}`, data)

export const deleteTransaction = (id) =>
  api.delete(`/transactions/${id}`)
