export type Role = 'MANAGER' | 'TRAINER' | 'CLIENT'

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
