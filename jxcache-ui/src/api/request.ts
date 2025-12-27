import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 创建请求实例
 * @param baseURL 基础地址
 */
export function createRequest(baseURL: string) {
  const instance = axios.create({
    baseURL,
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json'
    }
  })

  instance.interceptors.response.use(
    response => response.data,
    error => {
      const message = error.response?.data?.message || error.message || 'Request failed'
      ElMessage.error(message)
      return Promise.reject(error)
    }
  )

  return instance
}

export default createRequest
