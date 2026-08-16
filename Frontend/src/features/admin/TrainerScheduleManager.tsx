import { useEffect, useState } from 'react'
import { LoadingIndicator } from '../../components/LoadingIndicator'
import { getTrainerSchedule } from './api'
import type { TrainerScheduleDTO, WorkStatus } from './types'

const STATUS_LABEL: Record<WorkStatus, string> = {
  WORKING: 'Radi',
  HOLIDAY: 'Praznik',
  SICK_LEAVE: 'Bolovanje',
  VACATION: 'Odmor',
}

/**
 * MANAGER oversight of a single trainer's schedule - read-only. Creating shifts/unavailability
 * and deleting entries is TRAINER self-service only (features/schedule/TrainerSchedulePage.tsx) -
 * a MANAGER used to be able to write here too, but that's a stale capability from before the
 * self-service side existed; see AGENTS.md "Upgrade: manager schedule-write removal decisions".
 */
export function TrainerScheduleManager({ trainerId }: { trainerId: number }) {
  const [entries, setEntries] = useState<TrainerScheduleDTO[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    getTrainerSchedule(trainerId)
      .then(setEntries)
      .finally(() => setLoading(false))
  }, [trainerId])

  return (
    <div className="mt-3 space-y-3 rounded-xl border border-slate-800 bg-slate-950/60 p-4">
      {loading ? (
        <LoadingIndicator className="text-sm text-slate-500" />
      ) : entries.length === 0 ? (
        <p className="text-sm text-slate-500">Nema unetog rasporeda.</p>
      ) : (
        <ul className="space-y-1 text-sm">
          {entries.map((e) => (
            <li key={e.id} className="rounded-lg bg-slate-900/60 px-3 py-1.5">
              <span className="text-slate-300">
                {e.date} · {e.startTime.slice(0, 5)}–{e.endTime.slice(0, 5)} ·{' '}
                <span className="text-slate-400">{STATUS_LABEL[e.status]}</span>
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
