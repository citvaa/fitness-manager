import { http } from '../lib/http'
import type { AuthResponse } from './types'

export async function login(email: string, password: string): Promise<AuthResponse> {
  const res = await http.post<AuthResponse>('/api/user/login', { email, password })
  return res.data
}

/** Completes an invite-based registration (see AGENTS.md "Upgrade: Faza 6 decisions"). */
export function completeRegistration(registrationKey: string, password: string) {
  return http.post('/api/user/register', { registrationKey, password })
}

export function requestPasswordReset(email: string) {
  return http.post('/api/user/forgot-password', null, { params: { email } })
}

export function resetPassword(resetKey: string, password: string) {
  return http.post('/api/user/reset-password', { resetKey, password })
}
