import { useEffect, useState } from 'react'
import { getDailySchedule } from './api'
import type { AppointmentDTO } from './types'

const SESSION_TYPE_LABEL: Record<string, string> = { INDIVIDUAL: 'Individualni', GROUP: 'Grupni' }

export function DailySchedulePage() {
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [appointments, setAppointments] = useState<AppointmentDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    getDailySchedule(date)
      .then((schedule) => setAppointments(schedule.appointments ?? []))
      .catch(() => setError('Učitavanje rasporeda nije uspelo.'))
      .finally(() => setLoading(false))
  }, [date])

  const sorted = [...appointments].sort((a, b) => a.startTime.localeCompare(b.startTime))

  return (
    <div className="p-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-100">Dnevni raspored</h1>
      <p className="mb-6 text-sm text-slate-500">Svi termini zakazani za izabrani dan.</p>

      <div className="mb-4">
        <input
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none focus:border-brand-500"
        />
      </div>

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        {loading ? (
          <p className="text-sm text-slate-500">Učitavanje...</p>
        ) : error ? (
          <p className="text-sm text-red-400">{error}</p>
        ) : sorted.length === 0 ? (
          <p className="text-sm text-slate-500">Nema zakazanih termina za ovaj dan.</p>
        ) : (
          <ul className="space-y-2">
            {sorted.map((a) => (
              <li
                key={a.id}
                className="rounded-xl border border-slate-800 bg-slate-950/60 p-3 text-sm"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-medium text-slate-100">
                    {a.startTime.slice(0, 5)}–{a.endTime.slice(0, 5)}
                  </span>
                  <span className="text-xs text-slate-500">
                    {a.session ? (SESSION_TYPE_LABEL[a.session.type] ?? a.session.type) : '—'}
                  </span>
                </div>
                <div className="mt-1 text-xs text-slate-400">
                  Trener: {a.trainer?.email ?? 'nedodeljen'}
                </div>
                <div className="mt-1 text-xs text-slate-400">
                  Klijenti:{' '}
                  {a.clients && a.clients.length > 0
                    ? a.clients.map((c) => c.email).join(', ')
                    : 'nema prijavljenih'}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
