import { useEffect } from 'react'
import { jwtDecode } from 'jwt-decode'
import { useAuthStore } from './store'
import { refreshAccessToken } from '../lib/http'
import type { AccessTokenClaims } from './types'

// Access tokens live 15 minutes (app.jwt.accessTokenExpiration in
// application.yaml) - refresh a minute early so a request in flight right
// at expiry doesn't get caught by a 401 round-trip.
const REFRESH_SKEW_MS = 60_000

/**
 * Mounted once near the root. Whenever the access token changes, schedules
 * a silent refresh shortly before it expires - so the reactive 401-triggered
 * refresh in lib/http.ts is a fallback, not the primary mechanism.
 */
export function RefreshScheduler() {
  const accessToken = useAuthStore((s) => s.accessToken)
  const refreshToken = useAuthStore((s) => s.refreshToken)

  useEffect(() => {
    if (!accessToken || !refreshToken) return

    let claims: AccessTokenClaims
    try {
      claims = jwtDecode<AccessTokenClaims>(accessToken)
    } catch {
      return
    }

    const msUntilRefresh = claims.exp * 1000 - Date.now() - REFRESH_SKEW_MS
    const timer = setTimeout(
      () => {
        void refreshAccessToken()
      },
      Math.max(msUntilRefresh, 0),
    )

    return () => clearTimeout(timer)
  }, [accessToken, refreshToken])

  return null
}
