import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { LoginPage } from './pages/LoginPage'
import { CompleteRegistrationPage } from './pages/CompleteRegistrationPage'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'
import { AppShell } from './layout/AppShell'
import { ProtectedRoute, RequireActiveRole } from './auth/ProtectedRoute'
import { RefreshScheduler } from './auth/RefreshScheduler'
import { HomeRedirect } from './pages/HomeRedirect'
import { RoomEditorPage } from './features/gym/RoomEditorPage'
import { LiveFloorPlanPage } from './features/gym/LiveFloorPlanPage'
import { ManagerInsightsPage } from './features/insights/ManagerInsightsPage'
import { TrainerProgressPage } from './features/progress/TrainerProgressPage'
import { ClientProgressPage } from './features/progress/ClientProgressPage'
import { AdminPage } from './features/admin/AdminPage'
import { TrainerSchedulePage } from './features/schedule/TrainerSchedulePage'
import { ManagerPaymentsPage } from './features/payments/ManagerPaymentsPage'
import { MyPaymentsPage } from './features/payments/MyPaymentsPage'
import { DailySchedulePage } from './features/calendar/DailySchedulePage'

export default function App() {
  return (
    <BrowserRouter>
      <RefreshScheduler />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register/complete" element={<CompleteRegistrationPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route path="/" element={<HomeRedirect />} />

            <Route element={<RequireActiveRole role="MANAGER" />}>
              <Route path="/manager/room-editor" element={<RoomEditorPage />} />
              <Route path="/manager/floor-plan" element={<LiveFloorPlanPage />} />
              <Route path="/manager/insights" element={<ManagerInsightsPage />} />
              <Route path="/manager/administracija" element={<AdminPage />} />
              <Route path="/manager/placanja" element={<ManagerPaymentsPage />} />
              <Route path="/manager/dnevni-raspored" element={<DailySchedulePage />} />
            </Route>

            <Route element={<RequireActiveRole role="TRAINER" />}>
              <Route path="/trainer" element={<TrainerProgressPage />} />
              <Route path="/trainer/raspored" element={<TrainerSchedulePage />} />
            </Route>

            <Route element={<RequireActiveRole role="CLIENT" />}>
              <Route path="/client" element={<ClientProgressPage />} />
              <Route path="/client/uplate" element={<MyPaymentsPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<HomeRedirect />} />
      </Routes>
    </BrowserRouter>
  )
}
