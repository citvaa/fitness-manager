// ADMIN is additive to MANAGER (a user never holds ADMIN alone) and is not a switchable active
// role like the other three - it only gates the MANAGER-role grant/revoke actions in the admin
// UI (see UsersTab/ManagersTab and AGENTS.md "Upgrade: manager-hierarchy decisions").
export type Role = 'MANAGER' | 'TRAINER' | 'CLIENT' | 'ADMIN'

export interface AuthResponse {
  accessToken: string
  accessTokenExpirationTime: string
  refreshToken: string
  refreshTokenExpirationTime: string
}

export interface AccessTokenClaims {
  sub: string
  email: string
  roles: Role[]
  exp: number
}

export interface AuthUser {
  id: number
  email: string
  roles: Role[]
}
