import { NavLink, Outlet } from 'react-router-dom'
import { useAuthStore } from '../auth/store'
import type { Role } from '../auth/types'
import clsx from 'clsx'

// ADMIN is additive to MANAGER, never a switchable active role on its own (see
// auth/types.ts) - it has no entry in ROLE_PRIORITY/the role switcher and these two maps'
// ADMIN keys are only here to satisfy Record<Role, ...>; they're never actually looked up.
const ROLE_LABEL: Record<Role, string> = {
  MANAGER: 'Menadžer',
  TRAINER: 'Trener',
  CLIENT: 'Klijent',
  ADMIN: 'Admin',
}

const NAV_BY_ROLE: Record<Role, { to: string; label: string }[]> = {
  MANAGER: [
    { to: '/manager/floor-plan', label: 'Plan teretane (live)' },
    { to: '/manager/room-editor', label: 'Editor sala' },
    { to: '/manager/insights', label: 'AI uvid' },
    { to: '/manager/administracija', label: 'Administracija' },
    { to: '/manager/placanja', label: 'Plaćanja' },
    { to: '/manager/dnevni-raspored', label: 'Dnevni raspored' },
  ],
  TRAINER: [
    { to: '/trainer', label: 'Praćenje napretka' },
    { to: '/trainer/raspored', label: 'Moj raspored' },
    { to: '/trainer/termini', label: 'Moji termini' },
  ],
  CLIENT: [
    { to: '/client', label: 'Moj napredak' },
    { to: '/client/uplate', label: 'Moje uplate' },
    { to: '/client/zakazivanje', label: 'Zakaži trening' },
    { to: '/client/moji-termini', label: 'Moji termini' },
  ],
  ADMIN: [],
}

export function AppShell() {
  const user = useAuthStore((s) => s.user)
  const activeRole = useAuthStore((s) => s.activeRole)
  const setActiveRole = useAuthStore((s) => s.setActiveRole)
  const clear = useAuthStore((s) => s.clear)

  if (!user || !activeRole) return null

  // ADMIN is additive permission, not a switchable area (see auth/types.ts) - exclude it from
  // the role switcher so an ADMIN+MANAGER account (the seed admin) doesn't get an "Admin" button
  // that would set activeRole to a role with no nav/routes of its own.
  const switchableRoles = user.roles.filter((r) => r !== 'ADMIN')

  return (
    <div className="flex min-h-screen bg-slate-950 text-slate-100">
      <aside className="flex w-64 flex-col border-r border-slate-800 bg-slate-900/40">
        <div className="flex items-center gap-2 px-5 py-5">
          <span className="text-xl">🏋️</span>
          <span className="font-semibold">Fitness Manager</span>
        </div>

        {switchableRoles.length > 1 && (
          <div className="mx-4 mb-4 rounded-lg border border-slate-800 bg-slate-950 p-1">
            <div className="grid grid-cols-1 gap-1">
              {switchableRoles.map((role) => (
                <button
                  key={role}
                  onClick={() => setActiveRole(role)}
                  className={clsx(
                    'rounded-md px-3 py-1.5 text-left text-sm transition',
                    role === activeRole
                      ? 'bg-brand-600 text-white'
                      : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200',
                  )}
                >
                  {ROLE_LABEL[role]}
                </button>
              ))}
            </div>
          </div>
        )}

        <nav className="flex-1 space-y-1 px-3">
          {NAV_BY_ROLE[activeRole].map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                clsx(
                  'block rounded-lg px-3 py-2 text-sm font-medium transition',
                  isActive
                    ? 'bg-slate-800 text-white'
                    : 'text-slate-400 hover:bg-slate-800/60 hover:text-slate-200',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-slate-800 px-4 py-4">
          <p className="truncate text-xs text-slate-500">{user.email}</p>
          <button
            onClick={clear}
            className="mt-2 w-full rounded-lg border border-slate-800 px-3 py-1.5 text-sm text-slate-300 transition hover:bg-slate-800"
          >
            Odjava
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  )
}
