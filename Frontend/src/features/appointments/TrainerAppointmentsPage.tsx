import { useEffect, useMemo, useState } from 'react'
import { isAxiosError } from 'axios'
import { MonthCalendar } from '../../components/MonthCalendar'
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
 * that day's appointments, upcoming or past. "Termini bez trenera" deliberately stays a flat,
 * date-sorted list rather than a calendar: those slots are typically scattered thinly across many
 * different future dates (any day a manager opened a slot for), so a day-picker would mostly show
 * empty calendars and force clicking through dates one at a time to find anything - a sorted list
 * surfaces all of them at a glance instead.
 *
 * The calendar-only history view turned out to be a regression, not an improvement (see AGENTS.md
 * "Upgrade: appointment/schedule history visibility decisions") - seeing what a trainer worked on
 * previously required navigating month-by-month and clicking each past date individually, with no
 * way to just see "everything I've done" at a glance. A collapsible "Istorija dodeljenih termina"
 * section (`showHistory` state, collapsed by default so it doesn't dominate the page) restores a
 * always-reachable, one-click, reverse-chronological flat list of every past assigned appointment,
 * alongside - not instead of - the calendar (which stays useful for "what do I have on this
 * specific date").
 */
export function TrainerAppointmentsPage() {
  const [mine, setMine] = useState<AppointmentDTO[]>([])
  const [unassigned, setUnassigned] = useState<AppointmentDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [selectedDate, setSelectedDate] = useState(todayIso())
  const [showHistory, setShowHistory] = useState(false)

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

  const upcomingUnassigned = useMemo(
    () => unassigned.filter(isUpcoming).sort((a, b) => (a.date + a.startTime).localeCompare(b.date + b.startTime)),
    [unassigned],
  )

  const highlightedDates = useMemo(() => new Set(mine.map((a) => a.date)), [mine])

  const pastMine = useMemo(
    () => mine.filter(isPast).sort((a, b) => (b.date + b.startTime).localeCompare(a.date + a.startTime)),
    [mine],
  )

  const visibleForDate = useMemo(
    () => mine.filter((a) => a.date === selectedDate).sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [mine, selectedDate],
  )

  function appointmentCard(a: AppointmentDTO, options?: { assignAction?: boolean; showDate?: boolean }) {
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
        <div className="mt-1 text-xs text-slate-400">
          {(options?.assignAction || options?.showDate) && <>{a.date} · </>}
          Soba: {a.room?.name ?? 'bez sobe'}
        </div>
        <div className="mt-1 text-xs text-slate-400">Klijenti: {clientList(a)}</div>
        <div className="mt-2">
          {options?.assignAction ? (
            <button
              onClick={() => handleAssign(a.id)}
              disabled={busyId === a.id}
              className="rounded-lg bg-brand-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-brand-500 disabled:opacity-60"
            >
              {busyId === a.id ? '...' : 'Prijavi se'}
            </button>
          ) : (
            isUpcoming(a) && (
              <button
                onClick={() => handleUnassign(a.id)}
                disabled={busyId === a.id}
                className="rounded-lg border border-red-900/50 px-2 py-0.5 text-xs text-red-300 hover:bg-red-950/40 disabled:opacity-60"
              >
                {busyId === a.id ? '...' : 'Otkaži dodelu'}
              </button>
            )
          )}
        </div>
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
            <p className="text-sm text-slate-500">Učitavanje...</p>
          ) : visibleForDate.length === 0 ? (
            <p className="text-sm text-slate-500">Nema dodeljenih termina za ovaj dan.</p>
          ) : (
            <ul className="max-h-[28rem] space-y-2 overflow-y-auto">
              {visibleForDate.map((a) => appointmentCard(a))}
            </ul>
          )}
        </div>
      </div>

      <div className="mb-6 rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Termini bez trenera</h3>
        {loading ? (
          <p className="text-sm text-slate-500">Učitavanje...</p>
        ) : upcomingUnassigned.length === 0 ? (
          <p className="text-sm text-slate-500">Trenutno nema slobodnih termina bez trenera.</p>
        ) : (
          <ul className="max-h-[28rem] space-y-2 overflow-y-auto">
            {upcomingUnassigned.map((a) => appointmentCard(a, { assignAction: true }))}
          </ul>
        )}
      </div>

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <div className="mb-3 flex items-center justify-between gap-2">
          <h3 className="text-sm font-semibold text-slate-300">
            Istorija dodeljenih termina ({pastMine.length})
          </h3>
          <button
            onClick={() => setShowHistory((s) => !s)}
            className="rounded-lg border border-slate-700 px-3 py-1 text-xs text-slate-300 hover:bg-slate-800"
          >
            {showHistory ? 'Sakrij' : 'Prikaži'}
          </button>
        </div>
        {showHistory &&
          (loading ? (
            <p className="text-sm text-slate-500">Učitavanje...</p>
          ) : pastMine.length === 0 ? (
            <p className="text-sm text-slate-500">Još nema odrađenih termina.</p>
          ) : (
            <ul className="max-h-[28rem] space-y-2 overflow-y-auto">
              {pastMine.map((a) => appointmentCard(a, { showDate: true }))}
            </ul>
          ))}
      </div>
    </div>
  )
}
