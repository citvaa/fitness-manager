import { useEffect, useMemo, useState } from 'react'
import { isAxiosError } from 'axios'
import { LoadingIndicator } from '../../components/LoadingIndicator'
import { MonthCalendar } from '../../components/MonthCalendar'
import { ClientCheckInPanel } from './ClientCheckInPanel'
import {
  assignSelfToAppointment,
  getAppointmentsWithoutTrainer,
  getMyAppointmentsAsTrainer,
  unassignSelfFromAppointment,
} from './api'
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

function isUpcoming(a: AppointmentDTO) {
  return !isPast(a)
}

function todayIso(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

function clientList(a: AppointmentDTO) {
  if (a.clients.length === 0) return 'nema prijavljenih'
  return a.clients.map((c) => c.email).join(', ')
}

/**
 * TRAINER self-service scheduling - see AGENTS.md "Upgrade: Faza 7 decisions". This is the
 * "marketplace" side of the appointment model: a trainer sees slots a MANAGER created without a
 * trainer yet (`GET /without-trainer`) and can self-assign (`POST /{id}/assign`), or drop a
 * future assignment they already hold (`DELETE /{id}/unassign`).
 *
 * Restructured (see AGENTS.md "Upgrade: trainer self-assign decisions"): "assigned to me" (both
 * upcoming and history) is now ONE calendar day-picker (MonthCalendar, same component as the
 * admin Termini tab) instead of two separate flat lists - a trainer picks a day and sees exactly
 * that day's appointments, upcoming or past.
 *
 * "Termini bez trenera" originally stayed a flat, date-sorted list of every upcoming open slot
 * rather than being filtered by the calendar, on the reasoning that open slots are scattered
 * thinly across many different future dates and a day-picker would mostly show empty days. That
 * was reconsidered (see AGENTS.md "Upgrade: termini-bez-trenera calendar filtering decision"): the
 * page reads more consistently with ONE selected day driving both sections, and the calendar's dot
 * indicators already tell a trainer which days have anything to look at (for the "assigned to me"
 * section) - "Termini bez trenera" now filters to `selectedDate` exactly like "Termini za
 * {selectedDate}" above it, using the same calendar and the same selected day.
 *
 * "Započni trening" (assigned-to-me cards only) toggles a per-appointment `ClientCheckInPanel`
 * with Check-in/Check-out per roster client, backed by the pre-existing room check-in endpoints -
 * see AGENTS.md "Upgrade: trainer check-in decisions".
 */
export function TrainerAppointmentsPage() {
  const [mine, setMine] = useState<AppointmentDTO[]>([])
  const [unassigned, setUnassigned] = useState<AppointmentDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [selectedDate, setSelectedDate] = useState(todayIso())
  const [checkInPanelId, setCheckInPanelId] = useState<number | null>(null)

  async function reload() {
    setLoading(true)
    try {
      const [mineRes, unassignedRes] = await Promise.all([
        getMyAppointmentsAsTrainer(),
        getAppointmentsWithoutTrainer(),
      ])
      setMine(mineRes)
      setUnassigned(unassignedRes)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
  }, [])

  async function handleAssign(id: number) {
    setError(null)
    setBusyId(id)
    try {
      await assignSelfToAppointment(id)
      await reload()
    } catch (err) {
      setError(extractErrorMessage(err, 'Dodela nije uspela.'))
    } finally {
      setBusyId(null)
    }
  }

  async function handleUnassign(id: number) {
    setError(null)
    setBusyId(id)
    try {
      await unassignSelfFromAppointment(id)
      await reload()
    } catch (err) {
      setError(extractErrorMessage(err, 'Otkazivanje dodele nije uspelo.'))
    } finally {
      setBusyId(null)
    }
  }

  const highlightedDates = useMemo(() => new Set(mine.map((a) => a.date)), [mine])

  const visibleForDate = useMemo(
    () => mine.filter((a) => a.date === selectedDate).sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [mine, selectedDate],
  )

  const unassignedForDate = useMemo(
    () => unassigned.filter((a) => a.date === selectedDate).sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [unassigned, selectedDate],
  )

  function appointmentCard(a: AppointmentDTO, options?: { assignAction?: boolean }) {
    return (
      <li key={a.id} className="rounded-xl border border-slate-800 bg-slate-950/60 p-3 text-sm">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className="font-medium text-slate-100">
            {a.startTime.slice(0, 5)}–{a.endTime.slice(0, 5)}
          </span>
          <span className="text-xs text-slate-500">
            {SESSION_TYPE_LABEL[a.session.type] ?? a.session.type} · {a.clients.length}/{a.session.maxParticipants}
          </span>
        </div>
        <div className="mt-1 text-xs text-slate-400">Soba: {a.room?.name ?? 'bez sobe'}</div>
        <div className="mt-1 text-xs text-slate-400">Klijenti: {clientList(a)}</div>
        <div className="mt-2 flex flex-wrap items-center gap-2">
          {options?.assignAction ? (
            <button
              onClick={() => handleAssign(a.id)}
              disabled={busyId === a.id}
              className="rounded-lg bg-brand-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-brand-500 disabled:opacity-60"
            >
              {busyId === a.id ? '...' : 'Prijavi se'}
            </button>
          ) : (
            <>
              <button
                onClick={() => setCheckInPanelId((id) => (id === a.id ? null : a.id))}
                className="rounded-lg border border-slate-700 px-2 py-0.5 text-xs text-slate-300 hover:bg-slate-800"
              >
                {checkInPanelId === a.id ? 'Sakrij klijente' : 'Započni trening'}
              </button>
              {isUpcoming(a) && (
                <button
                  onClick={() => handleUnassign(a.id)}
                  disabled={busyId === a.id}
                  className="rounded-lg border border-red-900/50 px-2 py-0.5 text-xs text-red-300 hover:bg-red-950/40 disabled:opacity-60"
                >
                  {busyId === a.id ? '...' : 'Otkaži dodelu'}
                </button>
              )}
            </>
          )}
        </div>
        {!options?.assignAction && checkInPanelId === a.id && <ClientCheckInPanel appointment={a} />}
      </li>
    )
  }

  return (
    <div className="p-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-100">Moji termini</h1>
      <p className="mb-6 text-sm text-slate-500">
        Termini na koje si dodeljen, i slobodni termini bez trenera na koje se možeš sam prijaviti.
      </p>

      {error && <p className="mb-4 rounded-lg bg-red-950/60 px-3 py-2 text-sm text-red-300">{error}</p>}

      <div className="mb-6 grid gap-4 lg:grid-cols-[auto,1fr]">
        <MonthCalendar value={selectedDate} onChange={setSelectedDate} highlightedDates={highlightedDates} />

        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
          <h3 className="mb-3 text-sm font-semibold text-slate-300">
            Termini za {selectedDate} ({visibleForDate.length})
          </h3>
          {loading ? (
            <LoadingIndicator className="text-sm text-slate-500" />
          ) : visibleForDate.length === 0 ? (
            <p className="text-sm text-slate-500">Nema dodeljenih termina za ovaj dan.</p>
          ) : (
            <ul className="max-h-[28rem] space-y-2 overflow-y-auto">
              {visibleForDate.map((a) => appointmentCard(a))}
            </ul>
          )}
        </div>
      </div>

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-3 text-sm font-semibold text-slate-300">
          Termini bez trenera za {selectedDate} ({unassignedForDate.length})
        </h3>
        {loading ? (
          <LoadingIndicator className="text-sm text-slate-500" />
        ) : unassignedForDate.length === 0 ? (
          <p className="text-sm text-slate-500">Nema slobodnih termina bez trenera za ovaj dan.</p>
        ) : (
          <ul className="max-h-[28rem] space-y-2 overflow-y-auto">
            {unassignedForDate.map((a) => appointmentCard(a, { assignAction: true }))}
          </ul>
        )}
      </div>
    </div>
  )
}
