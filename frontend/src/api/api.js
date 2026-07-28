import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
})

export const convertFile = (file, type) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  return api.post('/convert', formData, {
    responseType: 'blob',
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const getConversionHistory = () => api.get('/convert/history')

export const generateCoverPage = (payload) =>
  api.post('/coverpage/generate', payload, { responseType: 'blob' })

export const saveCoverPage = (payload) => api.post('/coverpage/save', payload)

export const listSavedCoverPages = () => api.get('/coverpage/saved')

export const getSavedCoverPage = (id) => api.get(`/coverpage/saved/${id}`)

export const deleteSavedCoverPage = (id) => api.delete(`/coverpage/saved/${id}`)

export default api
