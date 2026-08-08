import { lazy, Suspense, useEffect } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuthStore } from './auth/authStore'
import { AppShell } from './components/AppShell'
import { LoginPage } from './pages/LoginPage'
import { AccountFlowPage } from './pages/AccountFlowPage'
import { refreshAccessToken } from './api/client'
import { decodeJwt } from './auth/token'
const EditorPage=lazy(()=>import('./pages/EditorPage').then(m=>({default:m.EditorPage})))
const LivePlanPage=lazy(()=>import('./pages/LivePlanPage').then(m=>({default:m.LivePlanPage})))
const ManagerInsightsPage=lazy(()=>import('./pages/ManagerInsightsPage').then(m=>({default:m.ManagerInsightsPage})))
const ProgressPage=lazy(()=>import('./pages/ProgressPage').then(m=>({default:m.ProgressPage})))
const AdministrationPage=lazy(()=>import('./pages/AdministrationPage').then(m=>({default:m.AdministrationPage})))
const SchedulesPage=lazy(()=>import('./pages/SchedulesPage').then(m=>({default:m.SchedulesPage})))
function ProtectedShell(){const session=useAuthStore(s=>s.session);const location=useLocation();useEffect(()=>{if(!session)return;const refreshIn=Math.max(0,decodeJwt(session.accessToken).exp*1000-Date.now()-60_000);const timer=window.setTimeout(()=>void refreshAccessToken(),refreshIn);return()=>window.clearTimeout(timer)},[session?.accessToken]);return session?<AppShell/>:<Navigate to="/login" replace state={{from:location.pathname}}/>}
function RoleHome(){const role=useAuthStore(s=>s.session?.activeRole);return <Navigate to={role==='MANAGER'?'/app/live':'/app/progress'} replace/>}
function ManagerOnly({children}:{children:React.ReactNode}){return useAuthStore(s=>s.session?.activeRole)==='MANAGER'?children:<Navigate to="/app/progress" replace/>}
function ProgressOnly({children}:{children:React.ReactNode}){const role=useAuthStore(s=>s.session?.activeRole);return role==='TRAINER'||role==='CLIENT'?children:<Navigate to="/app" replace/>}
const loading=(text:string)=><div className="loading-page">{text}</div>
export default function App(){return <Routes><Route path="/login" element={<LoginPage/>}/><Route path="/complete-registration" element={<AccountFlowPage mode="register"/>}/><Route path="/forgot-password" element={<AccountFlowPage mode="forgot"/>}/><Route path="/reset-password" element={<AccountFlowPage mode="reset"/>}/><Route path="/app" element={<ProtectedShell/>}><Route index element={<RoleHome/>}/><Route path="live" element={<ManagerOnly><Suspense fallback={loading('Učitavanje plana…')}><LivePlanPage/></Suspense></ManagerOnly>}/><Route path="editor" element={<ManagerOnly><Suspense fallback={loading('Učitavanje editora…')}><EditorPage/></Suspense></ManagerOnly>}/><Route path="insights" element={<ManagerOnly><Suspense fallback={loading('Učitavanje uvida…')}><ManagerInsightsPage/></Suspense></ManagerOnly>}/><Route path="administration" element={<ManagerOnly><Suspense fallback={loading('Učitavanje administracije…')}><AdministrationPage/></Suspense></ManagerOnly>}/><Route path="schedules" element={<Suspense fallback={loading('Učitavanje rasporeda…')}><SchedulesPage/></Suspense>}/><Route path="progress" element={<ProgressOnly><Suspense fallback={loading('Učitavanje napretka…')}><ProgressPage/></Suspense></ProgressOnly>}/></Route><Route path="*" element={<Navigate to="/app" replace/>}/></Routes>}

