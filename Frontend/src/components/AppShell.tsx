import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../auth/authStore'
import { decodeJwt } from '../auth/token'
import type { Role } from '../types'

const roleLabels: Record<Role, string> = { MANAGER: 'Menadžer', TRAINER: 'Trener', CLIENT: 'Klijent' }

export function AppShell() {
  const session = useAuthStore((state) => state.session)!
  const setActiveRole = useAuthStore((state) => state.setActiveRole)
  const clear = useAuthStore((state) => state.clear)
  const navigate = useNavigate()
  const claims = decodeJwt(session.accessToken)
  const roles = claims.roles ?? []
  const manager = session.activeRole === 'MANAGER'
  return <div className="app-shell">
    <aside className="sidebar">
      <div className="sidebar-brand"><span>FM</span><div>Fitness<small>GymOS</small></div></div>
      <nav aria-label="Glavna navigacija">
        {manager ? <>
          <p>Upravljanje</p>
          <NavLink to="/app/live"><i>◉</i> Plan uživo</NavLink>
          <NavLink to="/app/editor"><i>⌗</i> Editor sala</NavLink>
        </> : <NavLink to="/app/coming-soon"><i>↗</i> Praćenje napretka</NavLink>}
      </nav>
      <div className="sidebar-bottom">
        {roles.length > 1 && <label>Aktivna oblast<select value={session.activeRole} onChange={(e)=>{setActiveRole(e.target.value as Role); navigate('/app')}}>{roles.map(role=><option key={role} value={role}>{roleLabels[role]}</option>)}</select></label>}
        <div className="profile-chip"><span>{claims.email?.slice(0,2).toUpperCase() ?? 'FM'}</span><div><strong>{roleLabels[session.activeRole]}</strong><small>{claims.email}</small></div><button aria-label="Odjavi se" title="Odjavi se" onClick={()=>{clear(); navigate('/login')}}>↪</button></div>
      </div>
    </aside>
    <div className="main-column"><Outlet /></div>
  </div>
}
