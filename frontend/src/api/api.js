import axios from 'axios'

const api = axios.create({
  baseURL: 'https://pdf-converter-backend-r81c.onrender.com',
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

// Added '/api' prefix here to match your backend controller mapping
export const generateCoverPage = (payload) =>
  api.post('/api/coverpage/generate', payload, { responseType: 'blob' })

export const saveCoverPage = (payload) => api.post('/api/coverpage/save', payload)

export const listSavedCoverPages = () => api.get('/api/coverpage/saved')

export const getSavedCoverPage = (id) => api.get(`/api/coverpage/saved/${id}`)

export const deleteSavedCoverPage = (id) => api.delete(`/api/coverpage/saved/${id}`)

export default api