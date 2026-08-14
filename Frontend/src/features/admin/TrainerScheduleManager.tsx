import { type FormEvent, useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import { DateInput } from '../../components/DateInput'
import {
  createTrainerSchedule,
  createTrainerUnavailability,
  deleteTrainerScheduleEntry,
  getTrainerSchedule,
} from './api'
import type { TrainerScheduleDTO, WorkStatus } from './types'

/** See the same helper in features/schedule/TrainerSchedulePage.tsx - the backend now returns a
 * real, specific validation message instead of a bare 500 (GlobalExceptionHandler). */
function extractErrorMessage(err: unknown, fallback: string): string {
  if (isAxiosError(err) && typeof err.response?.data?.message === 'string') {
    return err.response.data.message
  }
  return fallback
}

const STATUS_LABEL: Record<WorkStatus, string> = {
  WORKING: 'Radi',
  HOLIDAY: 'Praznik',
  SICK_LEAVE: 'Bolovanje',
  VACATION: 'Odmor',
}

/**
 * MANAGER oversight of a single trainer's schedule - view/add/delete, using the same
 * MANAGER-only endpoints TrainerScheduleController already had (create/unavailable) plus the
 * new GET/{trainerId} and DELETE/{id} added in this phase. See AGENTS.md
 * "Upgrade: Faza 6 decisions".
 */
export function TrainerScheduleManager({ trainerId }: { trainerId: number }) {
  const [entries, setEntries] = useState<TrainerScheduleDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [date, setDate] = useState('')
  const [startTime, setStartTime] = useState('09:00')
  const [endTime, setEndTime] = useState('17:00')

  const [unavailStart, setUnavailStart] = useState('')
  const [unavailEnd, setUnavailEnd] = useState('')
  const [unavailStatus, setUnavailStatus] = useState<WorkStatus>('VACATION')

  async function reload() {
    setLoading(true)
    try {
      setEntries(await getTrainerSchedule(trainerId))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trainerId])

  async function handleAddShift(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      await createTrainerSchedule({
        trainerId,
        date,
        startTime: `${startTime}:00`,
        endTime: `${endTime}:00`,
      })
      setDate('')
      await reload()
    } catch (err) {
      setError(
        extractErrorMessage(
          err,
          'Unos radnog vremena nije uspeo - proveri da li je datum u okviru radnog vremena teretane i da se ne preklapa sa postojećom smenom.',
        ),
      )
    }
  }

  async function handleAddUnavailability(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      await createTrainerUnavailability({
        trainerId,
        startDate: unavailStart,
        endDate: unavailEnd,
        status: unavailStatus,
      })
      setUnavailStart('')
      setUnavailEnd('')
      await reload()
    } catch (err) {
      setError(extractErrorMessage(err, 'Unos neradnog perioda nije uspeo.'))
    }
  }

  async function handleDelete(id: number) {
    await deleteTrainerScheduleEntry(id)
    await reload()
  }

  return (
    <div className="mt-3 space-y-4 rounded-xl border border-slate-800 bg-slate-950/60 p-4">
      <div className="grid gap-4 md:grid-cols-2">
        <form onSubmit={handleAddShift} className="space-y-2">
          <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-400">
            Nova smena
          </h4>
          <div className="flex flex-wrap gap-2">
            <DateInput
              required
              value={date}
              onChange={setDate}
              className="rounded-lg border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
            <input
              type="time"
              required
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
            <input
              type="time"
              required
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
            <button
              type="submit"
              className="rounded-lg bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-500"
            >
              Dodaj
            </button>
          </div>
        </form>

        <form onSubmit={handleAddUnavailability} className="space-y-2">
          <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-400">
            Neradni period
          </h4>
          <div className="flex flex-wrap gap-2">
            <DateInput
              required
              value={unavailStart}
              onChange={setUnavailStart}
              className="rounded-lg border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
            <DateInput
              required
              value={unavailEnd}
              onChange={setUnavailEnd}
              className="rounded-lg border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
            <select
              value={unavailStatus}
              onChange={(e) => setUnavailStatus(e.target.value as WorkStatus)}
              className="rounded-lg border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            >
              <option value="VACATION">Odmor</option>
              <option value="SICK_LEAVE">Bolovanje</option>
              <option value="HOLIDAY">Praznik</option>
            </select>
            <button
              type="submit"
              className="rounded-lg border border-slate-700 px-3 py-1.5 text-sm text-slate-300 hover:bg-slate-800"
            >
              Dodaj
            </button>
          </div>
        </form>
      </div>

      {error && <p className="text-xs text-red-400">{error}</p>}

      {loading ? (
        <p className="text-sm text-slate-500">Učitavanje...</p>
      ) : entries.length === 0 ? (
        <p className="text-sm text-slate-500">Nema unetog rasporeda.</p>
      ) : (
        <ul className="space-y-1 text-sm">
          {entries.map((e) => (
            <li
              key={e.id}
              className="flex items-center justify-between rounded-lg bg-slate-900/60 px-3 py-1.5"
            >
              <span className="text-slate-300">
                {e.date} · {e.startTime.slice(0, 5)}–{e.endTime.slice(0, 5)} ·{' '}
                <span className="text-slate-400">{STATUS_LABEL[e.status]}</span>
              </span>
              <button
                onClick={() => handleDelete(e.id)}
                className="rounded-lg border border-red-900/50 px-2 py-0.5 text-xs text-red-300 hover:bg-red-950/40"
              >
                Obriši
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
