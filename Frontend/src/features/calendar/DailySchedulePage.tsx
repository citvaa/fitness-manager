import { useEffect, useMemo, useState } from 'react'
import { MonthCalendar } from '../../components/MonthCalendar'
import { buildGymMutedReason } from '../../lib/gymAvailability'
import { getDailySchedule, getRoomsForFilter, getTrainersForFilter } from './api'
import { getTrainerSchedule } from '../admin/api'
import type { TrainerScheduleDTO } from '../admin/types'
import type { AppointmentDTO, RoomSummaryDTO, TrainerSummaryDTO } from './types'
import { LoadingIndicator } from '../../components/LoadingIndicator'

const SESSION_TYPE_LABEL: Record<string, string> = { INDIVIDUAL: 'Individualni', GROUP: 'Grupni' }
const WORK_STATUS_LABEL: Record<string, string> = {
  HOLIDAY: 'Praznik',
  SICK_LEAVE: 'Bolovanje',
  VACATION: 'Odmor',
}

function todayIso(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

/** Same calendar day-picker + filter pattern as the admin Termini tab
 * (features/admin/AppointmentsTab.tsx) - see AGENTS.md "Upgrade: fixed weekly appointment
 * decisions" for why both screens moved off a native `<input type="date">` + flat list. */
export function DailySchedulePage() {
  const [date, setDate] = useState(todayIso())
  const [appointments, setAppointments] = useState<AppointmentDTO[]>([])
  const [trainers, setTrainers] = useState<TrainerSummaryDTO[]>([])
  const [rooms, setRooms] = useState<RoomSummaryDTO[]>([])
  const [filterTrainerId, setFilterTrainerId] = useState('')
  const [filterRoomId, setFilterRoomId] = useState('')
  const [filterSessionType, setFilterSessionType] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [gymMutedReason, setGymMutedReason] = useState<((iso: string) => string | null) | null>(null)
  const [filterTrainerSchedule, setFilterTrainerSchedule] = useState<TrainerScheduleDTO[]>([])

  useEffect(() => {
    void buildGymMutedReason().then(setGymMutedReason)
  }, [])

  // Only fetched when a specific trainer is selected in the filter above - the calendar has no
  // single "current trainer" otherwise, unlike the TRAINER self-service pages. See AGENTS.md
  // "Upgrade: MonthCalendar unavailability decisions".
  useEffect(() => {
    if (!filterTrainerId) {
      setFilterTrainerSchedule([])
      return
    }
    let cancelled = false
    getTrainerSchedule(Number(filterTrainerId)).then((schedule) => {
      if (!cancelled) setFilterTrainerSchedule(schedule)
    })
    return () => {
      cancelled = true
    }
  }, [filterTrainerId])

  const getMutedReason = useMemo(() => {
    const nonWorkingByDate = new Map(
      filterTrainerSchedule.filter((e) => e.status !== 'WORKING').map((e) => [e.date, WORK_STATUS_LABEL[e.status]]),
    )
    return (iso: string) => {
      const own = nonWorkingByDate.get(iso)
      if (own) return `Trener nedostupan: ${own}`
      return gymMutedReason?.(iso) ?? null
    }
  }, [filterTrainerSchedule, gymMutedReason])

  useEffect(() => {
    Promise.all([getTrainersForFilter(), getRoomsForFilter()])
      .then(([t, r]) => {
        setTrainers(t)
        setRooms(r)
      })
      .catch(() => {
        /* filters are a convenience - a failure here shouldn't block the daily schedule itself */
      })
  }, [])

  useEffect(() => {
    setLoading(true)
    setError(null)
    getDailySchedule(date)
      .then((schedule) => setAppointments(schedule.appointments ?? []))
      .catch(() => setError('Učitavanje rasporeda nije uspelo.'))
      .finally(() => setLoading(false))
  }, [date])

  const visible = appointments
    .filter((a) => !filterTrainerId || String(a.trainer?.id ?? '') === filterTrainerId)
    .filter((a) => !filterRoomId || String(a.room?.id ?? '') === filterRoomId)
    .filter((a) => !filterSessionType || a.session?.type === filterSessionType)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))

  return (
    <div className="p-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-100">Dnevni raspored</h1>
      <p className="mb-6 text-sm text-slate-500">Svi termini zakazani za izabrani dan.</p>

      <div className="grid gap-4 lg:grid-cols-[auto,1fr]">
        <MonthCalendar value={date} onChange={setDate} getMutedReason={getMutedReason} />

        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
          <h2 className="mb-3 text-sm font-semibold text-slate-300">
            Termini za {date} ({visible.length})
          </h2>

          <div className="mb-3 grid gap-2 sm:grid-cols-3">
            <select
              value={filterTrainerId}
              onChange={(e) => setFilterTrainerId(e.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-xs text-slate-100 outline-none focus:border-brand-500"
            >
              <option value="">Svi treneri</option>
              {trainers.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.email}
                </option>
              ))}
            </select>
            <select
              value={filterRoomId}
              onChange={(e) => setFilterRoomId(e.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-xs text-slate-100 outline-none focus:border-brand-500"
            >
              <option value="">Sve sobe</option>
              {rooms.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
            <select
              value={filterSessionType}
              onChange={(e) => setFilterSessionType(e.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-xs text-slate-100 outline-none focus:border-brand-500"
            >
              <option value="">Svi tipovi sesija</option>
              <option value="INDIVIDUAL">Individualni</option>
              <option value="GROUP">Grupni</option>
            </select>
          </div>

          {loading ? (
            <LoadingIndicator className="text-sm text-slate-500" />
          ) : error ? (
            <p className="text-sm text-red-400">{error}</p>
          ) : visible.length === 0 ? (
            <p className="text-sm text-slate-500">Nema termina za prikaz na izabrani dan/filtere.</p>
          ) : (
            <ul className="space-y-2">
              {visible.map((a) => (
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
                    Trener: {a.trainer?.email ?? 'nedodeljen'} · Soba: {a.room?.name ?? 'bez sobe'}
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
    </div>
  )
}
