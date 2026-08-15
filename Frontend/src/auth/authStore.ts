import { create } from 'zustand'
import type { AuthResponse, Role } from '../types'
import { decodeJwt, tokenExpiresSoon } from './token'

const STORAGE_KEY = 'fitness-manager-auth'
const OPERATIONAL_ROLES: Role[] = ['MANAGER', 'TRAINER', 'CLIENT']

const operationalRoles = (token: string) => (decodeJwt(token).roles ?? []).filter(role => OPERATIONAL_ROLES.includes(role))

interface StoredAuth extends AuthResponse { activeRole: Role }
interface AuthState {
  session: StoredAuth | null
  setSession: (response: AuthResponse) => void
  setActiveRole: (role: Role) => void
  clear: () => void
}

function readStored(): StoredAuth | null {
  try {
    const value = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null') as StoredAuth | null
    if (!value?.refreshToken || tokenExpiresSoon(value.refreshToken, 0)) return null
    const roles = operationalRoles(value.accessToken)
    if (!roles.length) return null
    return { ...value, activeRole: roles.includes(value.activeRole) ? value.activeRole : roles[0] }
  } catch { return null }
}

export const useAuthStore = create<AuthState>((set, get) => ({
  session: readStored(),
  setSession: (response) => {
    const previousRole = get().session?.activeRole
    const roles = operationalRoles(response.accessToken)
    const activeRole = previousRole && roles.includes(previousRole) ? previousRole : (roles[0] ?? 'CLIENT')
    const session = { ...response, activeRole }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    set({ session })
  },
  setActiveRole: (activeRole) => {
    const current = get().session
    if (!current || !operationalRoles(current.accessToken).includes(activeRole)) return
    const session = { ...current, activeRole }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    set({ session })
  },
  clear: () => { localStorage.removeItem(STORAGE_KEY); set({ session: null }) },
}))

export const currentSession = () => useAuthStore.getState().session
