import { useEffect, useMemo, useState } from 'react'
import { isAxiosError } from 'axios'
import { LoadingIndicator } from '../../components/LoadingIndicator'
import { MonthCalendar } from '../../components/MonthCalendar'
import { getAvailableAppointments, reserveAppointment } from './api'
import type { AppointmentDTO } from './types'

const SESSION_TYPE_LABEL: Record<string, string> = { INDIVIDUAL: 'Individualni', GROUP: 'Grupni' }

function extractErrorMessage(err: unknown, fallback: string): string {
  if (isAxiosError(err) && typeof err.response?.data?.message === 'string') {
    return err.response.data.message
  }
  return fallback
}

function isUpcoming(a: AppointmentDTO) {
  return `${a.date}T${a.endTime}` >= new Date().toISOString()
}

function todayIso(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

/**
 * CLIENT self-service booking - see AGENTS.md "Upgrade: Faza 7 decisions". Restructured to a
 * MonthCalendar day-picker (same pattern as the admin Termini tab's `AppointmentsTab.tsx`) instead
 * of a flat table of every available appointment regardless of date - see AGENTS.md "Upgrade:
 * CLIENT calendar decisions".
 */
export function ClientBookingPage() {
  const [appointments, setAppointments] = useState<AppointmentDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [reservingId, setReservingId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [selectedDate, setSelectedDate] = useState(todayIso())

  async function reload() {
    setLoading(true)
    try {
      setAppointments(await getAvailableAppointments())
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
  }, [])

  async function handleReserve(id: number) {
    setError(null)
    setMessage(null)
    setReservingId(id)
    try {
      await reserveAppointment(id)
      setMessage('Termin je uspešno rezervisan.')
      await reload()
    } catch (err) {
      setError(extractErrorMessage(err, 'Rezervacija nije uspela.'))
    } finally {
      setReservingId(null)
    }
  }

  const upcoming = useMemo(() => appointments.filter(isUpcoming), [appointments])

  const highlightedDates = useMemo(() => new Set(upcoming.map((a) => a.date)), [upcoming])

  const visibleForDate = useMemo(
    () =>
      upcoming
        .filter((a) => a.date === selectedDate)
        .sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [upcoming, selectedDate],
  )

  return (
    <div className="p-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-100">Zakaži trening</h1>
      <p className="mb-6 text-sm text-slate-500">Dostupni termini sa slobodnim mestima.</p>

      {error && <p className="mb-4 rounded-lg bg-red-950/60 px-3 py-2 text-sm text-red-300">{error}</p>}
      {message && (
        <p className="mb-4 rounded-lg bg-emerald-950/60 px-3 py-2 text-sm text-emerald-300">{message}</p>
      )}

      <div className="grid gap-4 lg:grid-cols-[auto,1fr]">
        <MonthCalendar value={selectedDate} onChange={setSelectedDate} highlightedDates={highlightedDates} />

        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
          <h3 className="mb-3 text-sm font-semibold text-slate-300">
            Termini za {selectedDate} ({visibleForDate.length})
          </h3>
          {loading ? (
            <LoadingIndicator className="text-sm text-slate-500" />
          ) : visibleForDate.length === 0 ? (
            <p className="text-sm text-slate-500">Nema dostupnih termina za ovaj dan.</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-800 text-xs uppercase tracking-wide text-slate-500">
                  <th className="py-2 pr-4">Vreme</th>
                  <th className="py-2 pr-4">Tip</th>
                  <th className="py-2 pr-4">Trener</th>
                  <th className="py-2 pr-4">Slobodna mesta</th>
                  <th className="py-2 pr-4" />
                </tr>
              </thead>
              <tbody>
                {visibleForDate.map((a) => (
                  <tr key={a.id} className="border-b border-slate-800/60">
                    <td className="py-2 pr-4 text-slate-300">
                      {a.startTime.slice(0, 5)}–{a.endTime.slice(0, 5)}
                    </td>
                    <td className="py-2 pr-4 text-slate-400">
                      {SESSION_TYPE_LABEL[a.session.type] ?? a.session.type}
                    </td>
                    <td className="py-2 pr-4 text-slate-400">{a.trainer?.email ?? 'Nije dodeljen'}</td>
                    <td className="py-2 pr-4 text-slate-400">
                      {a.session.maxParticipants - a.clients.length} / {a.session.maxParticipants}
                    </td>
                    <td className="py-2 pr-4">
                      <button
                        onClick={() => handleReserve(a.id)}
                        disabled={reservingId === a.id}
                        className="rounded-lg bg-brand-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-brand-500 disabled:opacity-60"
                      >
                        {reservingId === a.id ? 'Rezervišem...' : 'Rezerviši'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  )
}
