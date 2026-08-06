import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../auth/store'
import type { AuthResponse } from '../auth/types'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8088'

export const http = axios.create({ baseURL: API_BASE_URL })

// Plain axios instance (no interceptors) used for the refresh call itself,
// so refreshing never recurses into the 401 handler below.
const refreshClient = axios.create({ baseURL: API_BASE_URL })

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshPromise: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = useAuthStore.getState().refreshToken
  if (!refreshToken) return null

  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post<AuthResponse>('/api/user/login-refresh', null, { params: { refreshToken } })
      .then((res) => {
        useAuthStore.getState().setSession(res.data)
        return res.data.accessToken
      })
      .catch(() => {
        useAuthStore.getState().clear()
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

interface RetryableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean
}

http.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const config = error.config as RetryableConfig | undefined
    if (error.response?.status === 401 && config && !config._retried) {
      config._retried = true
      const newToken = await refreshAccessToken()
      if (newToken) {
        config.headers = config.headers ?? {}
        config.headers.Authorization = `Bearer ${newToken}`
        return http(config)
      }
    }
    return Promise.reject(error)
  },
)

export { refreshAccessToken }
