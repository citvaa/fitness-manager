import { Navigate } from 'react-router-dom'
import { useAuthStore } from '../auth/store'

const HOME_BY_ROLE: Record<string, string> = {
  MANAGER: '/manager/floor-plan',
  TRAINER: '/trainer',
  CLIENT: '/client',
}

/** Sends an authenticated user to the landing page for their active role. */
export function HomeRedirect() {
  const activeRole = useAuthStore((s) => s.activeRole)
  if (!activeRole) return <Navigate to="/login" replace />
  return <Navigate to={HOME_BY_ROLE[activeRole]} replace />
}
