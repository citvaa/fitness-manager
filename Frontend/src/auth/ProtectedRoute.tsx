import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from './store'
import type { Role } from './types'

export function ProtectedRoute() {
  const user = useAuthStore((s) => s.user)
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  return <Outlet />
}

/** Gate a subtree on the user's *active* role (see RoleSwitcher / store.ts). */
export function RequireActiveRole({ role }: { role: Role }) {
  const activeRole = useAuthStore((s) => s.activeRole)
  const user = useAuthStore((s) => s.user)

  if (!user) return <Navigate to="/login" replace />
  if (activeRole !== role) return <Navigate to="/" replace />
  return <Outlet />
}
