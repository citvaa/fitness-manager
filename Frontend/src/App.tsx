import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { LoginPage } from './pages/LoginPage'
import { AppShell } from './layout/AppShell'
import { ProtectedRoute, RequireActiveRole } from './auth/ProtectedRoute'
import { RefreshScheduler } from './auth/RefreshScheduler'
import { HomeRedirect } from './pages/HomeRedirect'
import { RoomEditorPage } from './features/gym/RoomEditorPage'
import { LiveFloorPlanPage } from './features/gym/LiveFloorPlanPage'
import { ManagerInsightsPage } from './features/insights/ManagerInsightsPage'
import { TrainerProgressPage } from './features/progress/TrainerProgressPage'
import { ClientProgressPage } from './features/progress/ClientProgressPage'

export default function App() {
  return (
    <BrowserRouter>
      <RefreshScheduler />
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route path="/" element={<HomeRedirect />} />

            <Route element={<RequireActiveRole role="MANAGER" />}>
              <Route path="/manager/room-editor" element={<RoomEditorPage />} />
              <Route path="/manager/floor-plan" element={<LiveFloorPlanPage />} />
              <Route path="/manager/insights" element={<ManagerInsightsPage />} />
            </Route>

            <Route element={<RequireActiveRole role="TRAINER" />}>
              <Route path="/trainer" element={<TrainerProgressPage />} />
            </Route>

            <Route element={<RequireActiveRole role="CLIENT" />}>
              <Route path="/client" element={<ClientProgressPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<HomeRedirect />} />
      </Routes>
    </BrowserRouter>
  )
}
