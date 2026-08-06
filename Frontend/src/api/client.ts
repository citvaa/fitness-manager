import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { AuthResponse } from '../types'
import { currentSession, useAuthStore } from '../auth/authStore'
import { tokenExpiresSoon } from '../auth/token'

export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8088'
export const api = axios.create({ baseURL: API_URL })
const publicClient = axios.create({ baseURL: API_URL })
let refreshPromise: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  if (refreshPromise) return refreshPromise
  const refreshToken = currentSession()?.refreshToken
  if (!refreshToken) throw new Error('Nema aktivne sesije')
  refreshPromise = publicClient
    .post<AuthResponse>('/api/user/login-refresh', null, { params: { refreshToken } })
    .then(({ data }) => { useAuthStore.getState().setSession(data); return data.accessToken })
    .catch((error) => { useAuthStore.getState().clear(); throw error })
    .finally(() => { refreshPromise = null })
  return refreshPromise
}

api.interceptors.request.use(async (config) => {
  const session = currentSession()
  if (!session) return config
  const token = tokenExpiresSoon(session.accessToken) ? await refreshAccessToken() : session.accessToken
  config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(undefined, async (error: AxiosError) => {
  const config = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
  if (error.response?.status !== 401 || !config || config._retried || !currentSession()) throw error
  config._retried = true
  config.headers.Authorization = `Bearer ${await refreshAccessToken()}`
  return api(config)
})

export async function login(email: string, password: string) {
  return (await publicClient.post<AuthResponse>('/api/user/login', { email, password })).data
}

export function errorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined
    return data?.message ?? (error.response ? `Server je vratio grešku ${error.response.status}.` : 'Backend nije dostupan na portu 8088.')
  }
  return error instanceof Error ? error.message : 'Došlo je do neočekivane greške.'
}
