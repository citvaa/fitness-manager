import { http } from '../lib/http'
import type { AuthResponse } from './types'

export async function login(email: string, password: string): Promise<AuthResponse> {
  const res = await http.post<AuthResponse>('/api/user/login', { email, password })
  return res.data
}
