import { create } from 'zustand'
import { jwtDecode } from 'jwt-decode'
import type { AccessTokenClaims, AuthResponse, AuthUser, Role } from './types'

const STORAGE_KEY = 'fm.auth'

interface StoredAuth {
  accessToken: string
  refreshToken: string
}

function readStorage(): StoredAuth | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as StoredAuth
  } catch {
    return null
  }
}

function writeStorage(auth: StoredAuth | null) {
  if (auth) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(auth))
  } else {
    localStorage.removeItem(STORAGE_KEY)
  }
}

function userFromToken(accessToken: string): AuthUser {
  const claims = jwtDecode<AccessTokenClaims>(accessToken)
  return {
    id: Number(claims.sub),
    email: claims.email,
    roles: claims.roles,
  }
}

// Which role's area is shown by default when a user holds more than one -
// see AGENTS.md "Upgrade: frontend decisions" for why this priority order
// (management action takes precedence over the areas it manages).
const ROLE_PRIORITY: Role[] = ['MANAGER', 'TRAINER', 'CLIENT']

export function defaultActiveRole(roles: Role[]): Role {
  return ROLE_PRIORITY.find((r) => roles.includes(r)) ?? roles[0]
}

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: AuthUser | null
  activeRole: Role | null
  isRefreshing: boolean
  setSession: (auth: AuthResponse) => void
  setActiveRole: (role: Role) => void
  clear: () => void
}

const initial = readStorage()
const initialUser = initial ? tryDecode(initial.accessToken) : null

function tryDecode(token: string): AuthUser | null {
  try {
    return userFromToken(token)
  } catch {
    return null
  }
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: initial?.accessToken ?? null,
  refreshToken: initial?.refreshToken ?? null,
  user: initialUser,
  activeRole: initialUser ? defaultActiveRole(initialUser.roles) : null,
  isRefreshing: false,
  setSession: (auth) => {
    const user = userFromToken(auth.accessToken)
    writeStorage({ accessToken: auth.accessToken, refreshToken: auth.refreshToken })
    set((state) => ({
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      user,
      // Keep the user's chosen active role across a silent refresh; only
      // pick a default the first time we see this session.
      activeRole: state.user ? state.activeRole : defaultActiveRole(user.roles),
    }))
  },
  setActiveRole: (role) => set({ activeRole: role }),
  clear: () => {
    writeStorage(null)
    set({ accessToken: null, refreshToken: null, user: null, activeRole: null })
  },
}))
