import type { JwtClaims } from '../types'

export function decodeJwt(token: string): JwtClaims {
  const payload = token.split('.')[1]
  if (!payload) throw new Error('Neispravan token')
  const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
  const decoded = decodeURIComponent(
    atob(normalized)
      .split('')
      .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
      .join(''),
  )
  return JSON.parse(decoded) as JwtClaims
}

export const tokenExpiresSoon = (token: string, bufferSeconds = 60) =>
  decodeJwt(token).exp * 1000 <= Date.now() + bufferSeconds * 1000
