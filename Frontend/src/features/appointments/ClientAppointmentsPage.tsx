import { useEffect, useMemo, useState } from 'react'
import { isAxiosError } from 'axios'
import { LoadingIndicator } from '../../components/LoadingIndicator'
import { MonthCalendar } from '../../components/MonthCalendar'
import { buildGymMutedReason } from '../../lib/gymAvailability'
import { cancelAppointment, getMyAppointmentsAsClient } from './api'
import type { AppointmentDTO } from './types'

const SESSION_TYPE_LABEL: Record<string, string> = { INDIVIDUAL: 'Individualni', GROUP: 'Grupni' }

function extractErrorMessage(err: unknown, fallback: string): string {
  if (isAxiosError(err) && typeof err.response?.data?.message === 'string') {
    return err.response.data.message
  }
  return fallback
}

function isPast(a: AppointmentDTO) {
  return `${a.date}T${a.endTime}` < new Date().toISOString()
}

function todayIso(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

/**
 * CLIENT's own reservation history (backend `GET /api/appointment/me` - see AGENTS.md "Upgrade:
 * Faza 7 decisions"). Cancellation is only offered on future entries - the backend enforces a
 * 24h-before-start cancellation deadline itself (`AppointmentServiceImpl.cancel`) and reports a
 * real message on violation, surfaced here rather than a generic fallback.
 *
 * Restructured to a MonthCalendar day-picker (same pattern as `AppointmentsTab.tsx`/
 * `TrainerAppointmentsPage.tsx`) instead of two always-visible "Budući termini"/"Istorija" flat
 * tables - one selected day now drives a single list, with the cancel button shown per-row only
 * for that row's own future/past status. See AGENTS.md "Upgrade: CLIENT calendar decisions".
 */
export function ClientAppointmentsPage() {
  const [appointments, setAppointments] = useState<AppointmentDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [cancelingId, setCancelingId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [selectedDate, setSelectedDate] = useState(todayIso())
  const [gymMutedReason, setGymMutedReason] = useState<((iso: string) => string | null) | null>(null)

  useEffect(() => {
    void buildGymMutedReason().then((fn) => setGymMutedReason(() => fn))
  }, [])

  async function reload() {
    setLoading(true)
    try {
      setAppointments(await getMyAppointmentsAsClient())
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
  }, [])

  async function handleCancel(id: number) {
    setError(null)
    setCancelingId(id)
    try {
      await cancelAppointment(id)
      await reload()
    } catch (err) {
      setError(
        extractErrorMessage(err, 'Otkazivanje nije uspelo - proveri da li je do termina ostalo bar 24 sata.'),
      )
    } finally {
      setCancelingId(null)
    }
  }

  const highlightedDates = useMemo(() => new Set(appointments.map((a) => a.date)), [appointments])

  const visibleForDate = useMemo(
    () =>
      appointments
        .filter((a) => a.date === selectedDate)
        .sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [appointments, selectedDate],
  )

  return (
    <div className="p-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-100">Moji termini</h1>
      <p className="mb-6 text-sm text-slate-500">
        Tvoji rezervisani termini - budući i istorija. Otkazivanje je moguće do 24h pre termina.
      </p>

      {error && <p className="mb-4 rounded-lg bg-red-950/60 px-3 py-2 text-sm text-red-300">{error}</p>}

      <div className="grid gap-4 lg:grid-cols-[auto,1fr]">
        <MonthCalendar
          value={selectedDate}
          onChange={setSelectedDate}
          highlightedDates={highlightedDates}
          getMutedReason={gymMutedReason ?? undefined}
        />

        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
          <h3 className="mb-3 text-sm font-semibold text-slate-300">
            Termini za {selectedDate} ({visibleForDate.length})
          </h3>
          {loading ? (
            <LoadingIndicator className="text-sm text-slate-500" />
          ) : visibleForDate.length === 0 ? (
            <p className="text-sm text-slate-500">Nema termina za ovaj dan.</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-800 text-xs uppercase tracking-wide text-slate-500">
                  <th className="py-2 pr-4">Vreme</th>
                  <th className="py-2 pr-4">Tip</th>
                  <th className="py-2 pr-4">Trener</th>
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
                    <td className="py-2 pr-4">
                      {!isPast(a) && (
                        <button
                          onClick={() => handleCancel(a.id)}
                          disabled={cancelingId === a.id}
                          className="rounded-lg border border-red-900/50 px-2 py-0.5 text-xs text-red-300 hover:bg-red-950/40 disabled:opacity-60"
                        >
                          {cancelingId === a.id ? 'Otkazujem...' : 'Otkaži'}
                        </button>
                      )}
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
