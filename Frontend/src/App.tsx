import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuthStore } from './auth/authStore'
import { AppShell } from './components/AppShell'
import { ComingSoonPage } from './pages/ComingSoonPage'
import { LoginPage } from './pages/LoginPage'
import { EditorPage } from './pages/EditorPage'
import { LivePlanPage } from './pages/LivePlanPage'

function ProtectedShell() {
  const session = useAuthStore((state)=>state.session)
  const location = useLocation()
  return session ? <AppShell /> : <Navigate to="/login" replace state={{from: location.pathname}} />
}

function RoleHome() {
  const role = useAuthStore((state)=>state.session?.activeRole)
  return <Navigate to={role === 'MANAGER' ? '/app/live' : '/app/coming-soon'} replace />
}

export default function App() {
  return <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/app" element={<ProtectedShell />}>
      <Route index element={<RoleHome />} />
      <Route path="coming-soon" element={<ComingSoonPage />} />
      <Route path="live" element={<LivePlanPage />} />
      <Route path="editor" element={<EditorPage />} />
    </Route>
    <Route path="*" element={<Navigate to="/app" replace />} />
  </Routes>
}
