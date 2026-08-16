import { useEffect, useMemo, useState } from 'react'
import { LoadingIndicator } from '../../components/LoadingIndicator'
import { MonthCalendar } from '../../components/MonthCalendar'
import { getTrainerSchedule } from './api'
import type { TrainerScheduleDTO, WorkStatus } from './types'

const STATUS_LABEL: Record<WorkStatus, string> = {
  WORKING: 'Radi',
  HOLIDAY: 'Praznik',
  SICK_LEAVE: 'Bolovanje',
  VACATION: 'Odmor',
}

function todayIso(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

/**
 * MANAGER oversight of a single trainer's schedule - read-only. Creating shifts/unavailability
 * and deleting entries is TRAINER self-service only (features/schedule/TrainerSchedulePage.tsx) -
 * a MANAGER used to be able to write here too, but that's a stale capability from before the
 * self-service side existed; see AGENTS.md "Upgrade: manager schedule-write removal decisions".
 *
 * Same MonthCalendar + per-day-entries pattern as TrainerSchedulePage.tsx (see AGENTS.md
 * "Upgrade: manager schedule-calendar decisions") - a flat <ul> of every entry was hard to scan
 * once a trainer had more than a handful of rows.
 */
export function TrainerScheduleManager({ trainerId }: { trainerId: number }) {
  const [entries, setEntries] = useState<TrainerScheduleDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedDate, setSelectedDate] = useState(todayIso())

  useEffect(() => {
    setLoading(true)
    getTrainerSchedule(trainerId)
      .then(setEntries)
      .finally(() => setLoading(false))
  }, [trainerId])

  const highlightedDates = useMemo(() => new Set(entries.map((e) => e.date)), [entries])

  const getMutedReason = useMemo(() => {
    const nonWorkingByDate = new Map(
      entries.filter((e) => e.status !== 'WORKING').map((e) => [e.date, STATUS_LABEL[e.status]]),
    )
    return (iso: string) => {
      const reason = nonWorkingByDate.get(iso)
      return reason ? `Nedostupnost: ${reason}` : null
    }
  }, [entries])

  const entriesForDate = useMemo(
    () => entries.filter((e) => e.date === selectedDate).sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [entries, selectedDate],
  )

  return (
    <div className="mt-3 grid gap-3 rounded-xl border border-slate-800 bg-slate-950/60 p-4 lg:grid-cols-[auto,1fr]">
      <MonthCalendar
        value={selectedDate}
        onChange={setSelectedDate}
        highlightedDates={highlightedDates}
        getMutedReason={getMutedReason}
      />

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Raspored za {selectedDate}</h3>
        {loading ? (
          <LoadingIndicator className="text-sm text-slate-500" />
        ) : entriesForDate.length === 0 ? (
          <p className="text-sm text-slate-500">Nema unetog rasporeda.</p>
        ) : (
          <ul className="space-y-1 text-sm">
            {entriesForDate.map((e) => (
              <li key={e.id} className="rounded-lg bg-slate-900/60 px-3 py-1.5">
                <span className="text-slate-300">
                  {e.startTime.slice(0, 5)}–{e.endTime.slice(0, 5)} ·{' '}
                  <span className="text-slate-400">{STATUS_LABEL[e.status]}</span>
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
