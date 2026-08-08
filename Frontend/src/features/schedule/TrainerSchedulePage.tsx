import { type FormEvent, useEffect, useState } from 'react'
import { createMySchedule, createMyUnavailability, deleteMyScheduleEntry, getMySchedule } from './api'
import type { TrainerScheduleDTO, WorkStatus } from './types'

const STATUS_LABEL: Record<WorkStatus, string> = {
  WORKING: 'Radi',
  HOLIDAY: 'Praznik',
  SICK_LEAVE: 'Bolovanje',
  VACATION: 'Odmor',
}

/**
 * TRAINER self-service - a trainer enters/manages only their own working hours and
 * unavailability, scoped server-side to the trainer resolved from the JWT (never a client-
 * supplied trainerId). See AGENTS.md "Upgrade: Faza 6 decisions".
 */
export function TrainerSchedulePage() {
  const [entries, setEntries] = useState<TrainerScheduleDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [date, setDate] = useState('')
  const [startTime, setStartTime] = useState('09:00')
  const [endTime, setEndTime] = useState('17:00')
  const [savingShift, setSavingShift] = useState(false)

  const [unavailStart, setUnavailStart] = useState('')
  const [unavailEnd, setUnavailEnd] = useState('')
  const [unavailStatus, setUnavailStatus] = useState<WorkStatus>('VACATION')
  const [savingUnavail, setSavingUnavail] = useState(false)

  async function reload() {
    setLoading(true)
    try {
      setEntries(await getMySchedule())
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
  }, [])

  async function handleAddShift(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSavingShift(true)
    try {
      await createMySchedule({ date, startTime: `${startTime}:00`, endTime: `${endTime}:00` })
      setDate('')
      await reload()
    } catch {
      setError(
        'Unos radnog vremena nije uspeo - proveri da li je datum u okviru radnog vremena teretane i da se ne preklapa sa postojećom smenom.',
      )
    } finally {
      setSavingShift(false)
    }
  }

  async function handleAddUnavailability(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSavingUnavail(true)
    try {
      await createMyUnavailability({ startDate: unavailStart, endDate: unavailEnd, status: unavailStatus })
      setUnavailStart('')
      setUnavailEnd('')
      await reload()
    } catch {
      setError('Unos neradnog perioda nije uspeo.')
    } finally {
      setSavingUnavail(false)
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Obrisati ovaj unos rasporeda?')) return
    await deleteMyScheduleEntry(id)
    await reload()
  }

  return (
    <div className="p-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-100">Moj raspored</h1>
      <p className="mb-6 text-sm text-slate-500">
        Uneseno radno vreme i neradni periodi vide se i menadžeru, ali ih menjaš isključivo ti.
      </p>

      <div className="grid gap-4 md:grid-cols-2">
        <form
          onSubmit={handleAddShift}
          className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4"
        >
          <h3 className="mb-3 text-sm font-semibold text-slate-300">Nova smena</h3>
          <div className="space-y-3">
            <label className="block text-xs text-slate-400">
              Datum
              <input
                type="date"
                required
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
              />
            </label>
            <div className="flex gap-3">
              <label className="block flex-1 text-xs text-slate-400">
                Od
                <input
                  type="time"
                  required
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
                />
              </label>
              <label className="block flex-1 text-xs text-slate-400">
                Do
                <input
                  type="time"
                  required
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
                />
              </label>
            </div>
          </div>
          <button
            type="submit"
            disabled={savingShift}
            className="mt-4 w-full rounded-lg bg-brand-600 px-3 py-2 text-sm font-medium text-white hover:bg-brand-500 disabled:opacity-60"
          >
            {savingShift ? 'Čuvanje...' : 'Dodaj smenu'}
          </button>
        </form>

        <form
          onSubmit={handleAddUnavailability}
          className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4"
        >
          <h3 className="mb-3 text-sm font-semibold text-slate-300">Neradni period</h3>
          <div className="space-y-3">
            <div className="flex gap-3">
              <label className="block flex-1 text-xs text-slate-400">
                Od datuma
                <input
                  type="date"
                  required
                  value={unavailStart}
                  onChange={(e) => setUnavailStart(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
                />
              </label>
              <label className="block flex-1 text-xs text-slate-400">
                Do datuma
                <input
                  type="date"
                  required
                  value={unavailEnd}
                  onChange={(e) => setUnavailEnd(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
                />
              </label>
            </div>
            <label className="block text-xs text-slate-400">
              Razlog
              <select
                value={unavailStatus}
                onChange={(e) => setUnavailStatus(e.target.value as WorkStatus)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
              >
                <option value="VACATION">Odmor</option>
                <option value="SICK_LEAVE">Bolovanje</option>
                <option value="HOLIDAY">Praznik</option>
              </select>
            </label>
          </div>
          <button
            type="submit"
            disabled={savingUnavail}
            className="mt-4 w-full rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 hover:bg-slate-800 disabled:opacity-60"
          >
            {savingUnavail ? 'Čuvanje...' : 'Prijavi neradni period'}
          </button>
        </form>
      </div>

      {error && (
        <p className="mt-4 rounded-lg bg-red-950/60 px-3 py-2 text-sm text-red-300">{error}</p>
      )}

      <div className="mt-6 rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Moji uneti termini</h3>
        {loading ? (
          <p className="text-sm text-slate-500">Učitavanje...</p>
        ) : entries.length === 0 ? (
          <p className="text-sm text-slate-500">Još ništa nije uneto.</p>
        ) : (
          <ul className="space-y-1 text-sm">
            {entries.map((e) => (
              <li
                key={e.id}
                className="flex items-center justify-between rounded-lg bg-slate-950/60 px-3 py-2"
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
    </div>
  )
}
