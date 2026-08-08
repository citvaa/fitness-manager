import { useState } from 'react'
import clsx from 'clsx'
import { UsersTab } from './UsersTab'
import { TrainersTab } from './TrainersTab'
import { ClientsTab } from './ClientsTab'
import { GymScheduleHolidaysTab } from './GymScheduleHolidaysTab'
import { AppointmentsTab } from './AppointmentsTab'

type Tab = 'users' | 'trainers' | 'clients' | 'schedule' | 'appointments'

const TABS: { key: Tab; label: string }[] = [
  { key: 'users', label: 'Korisnici' },
  { key: 'trainers', label: 'Treneri' },
  { key: 'clients', label: 'Klijenti' },
  { key: 'schedule', label: 'Radno vreme i praznici' },
  { key: 'appointments', label: 'Termini' },
]

/**
 * Client-side tabs rather than sub-routes - keeps this phase's routing additions to one entry
 * (/manager/administracija), matching App.tsx's existing flat-route-per-page convention while
 * still giving each concern (users/trainers/clients/schedule) its own uncluttered view. See
 * AGENTS.md "Upgrade: Faza 6 decisions".
 */
export function AdminPage() {
  const [tab, setTab] = useState<Tab>('users')

  return (
    <div className="p-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-100">Administracija</h1>
      <p className="mb-6 text-sm text-slate-500">
        Korisnici, treneri, klijenti, radno vreme teretane, praznici i termini.
      </p>

      <div className="mb-6 flex gap-1 border-b border-slate-800">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={clsx(
              'rounded-t-lg px-4 py-2 text-sm font-medium transition',
              tab === t.key
                ? 'border-b-2 border-brand-500 text-white'
                : 'text-slate-400 hover:text-slate-200',
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'users' && <UsersTab />}
      {tab === 'trainers' && <TrainersTab />}
      {tab === 'clients' && <ClientsTab />}
      {tab === 'schedule' && <GymScheduleHolidaysTab />}
      {tab === 'appointments' && <AppointmentsTab />}
    </div>
  )
}
