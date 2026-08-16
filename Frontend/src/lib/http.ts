import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../auth/store'
import type { AuthResponse } from '../auth/types'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8088'

export const http = axios.create({ baseURL: API_BASE_URL })

// Plain axios instance (no interceptors) used for the refresh call itself,
// so refreshing never recurses into the 401 handler below.
const refreshClient = axios.create({ baseURL: API_BASE_URL })

// Global in-flight request counter, backing the small floating activity indicator in AppShell
// (components/GlobalActivityIndicator.tsx) - a page-level LoadingIndicator only ever covers one
// screen's own fetch, so a background call (e.g. a mutation, or another panel's refresh) had no
// visible sign anything was happening at all. A plain module-level subscriber list rather than a
// store dependency, since this is the one cross-cutting piece of UI state that belongs next to
// the axios instance itself, not a feature.
let activeRequestCount = 0
const activeRequestListeners = new Set<(count: number) => void>()

function setActiveRequestCount(count: number) {
  activeRequestCount = count
  activeRequestListeners.forEach((listener) => listener(count))
}

export function subscribeToActiveRequests(listener: (count: number) => void): () => void {
  activeRequestListeners.add(listener)
  listener(activeRequestCount)
  return () => {
    activeRequestListeners.delete(listener)
  }
}

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  setActiveRequestCount(activeRequestCount + 1)
  return config
})

http.interceptors.response.use(
  (res) => {
    setActiveRequestCount(activeRequestCount - 1)
    return res
  },
  (error) => {
    setActiveRequestCount(activeRequestCount - 1)
    return Promise.reject(error)
  },
)

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
