import { lazy, Suspense, useEffect } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuthStore } from './auth/authStore'
import { AppShell } from './components/AppShell'
import { ComingSoonPage } from './pages/ComingSoonPage'
import { LoginPage } from './pages/LoginPage'
import { refreshAccessToken } from './api/client'
import { decodeJwt } from './auth/token'
const EditorPage=lazy(()=>import('./pages/EditorPage').then(m=>({default:m.EditorPage})))
const LivePlanPage=lazy(()=>import('./pages/LivePlanPage').then(m=>({default:m.LivePlanPage})))
const ManagerInsightsPage=lazy(()=>import('./pages/ManagerInsightsPage').then(m=>({default:m.ManagerInsightsPage})))
function ProtectedShell(){const session=useAuthStore(s=>s.session);const location=useLocation();useEffect(()=>{if(!session)return;const refreshIn=Math.max(0,decodeJwt(session.accessToken).exp*1000-Date.now()-60_000);const timer=window.setTimeout(()=>{void refreshAccessToken()},refreshIn);return()=>window.clearTimeout(timer)},[session?.accessToken]);return session?<AppShell/>:<Navigate to="/login" replace state={{from:location.pathname}}/>}
function RoleHome(){const role=useAuthStore(s=>s.session?.activeRole);return <Navigate to={role==='MANAGER'?'/app/live':'/app/coming-soon'} replace/>}
function ManagerOnly({children}:{children:React.ReactNode}){return useAuthStore(s=>s.session?.activeRole)==='MANAGER'?children:<Navigate to="/app/coming-soon" replace/>}
export default function App(){return <Routes><Route path="/login" element={<LoginPage/>}/><Route path="/app" element={<ProtectedShell/>}><Route index element={<RoleHome/>}/><Route path="coming-soon" element={<ComingSoonPage/>}/><Route path="live" element={<ManagerOnly><Suspense fallback={<div className="loading-page">Učitavanje plana…</div>}><LivePlanPage/></Suspense></ManagerOnly>}/><Route path="editor" element={<ManagerOnly><Suspense fallback={<div className="loading-page">Učitavanje editora…</div>}><EditorPage/></Suspense></ManagerOnly>}/><Route path="insights" element={<ManagerOnly><Suspense fallback={<div className="loading-page">Učitavanje uvida…</div>}><ManagerInsightsPage/></Suspense></ManagerOnly>}/></Route><Route path="*" element={<Navigate to="/app" replace/>}/></Routes>}
