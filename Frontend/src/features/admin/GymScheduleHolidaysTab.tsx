import { type FormEvent, useEffect, useState } from 'react'
import { createHoliday, getGymSchedule, getHolidays, upsertGymScheduleDay } from './api'
import type { DayOfWeek, GymScheduleDTO, HolidayDTO } from './types'

const DAYS: { value: DayOfWeek; label: string }[] = [
  { value: 'MONDAY', label: 'Ponedeljak' },
  { value: 'TUESDAY', label: 'Utorak' },
  { value: 'WEDNESDAY', label: 'Sreda' },
  { value: 'THURSDAY', label: 'Četvrtak' },
  { value: 'FRIDAY', label: 'Petak' },
  { value: 'SATURDAY', label: 'Subota' },
  { value: 'SUNDAY', label: 'Nedelja' },
]

export function GymScheduleHolidaysTab() {
  const [schedule, setSchedule] = useState<GymScheduleDTO[]>([])
  const [holidays, setHolidays] = useState<HolidayDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [savingDay, setSavingDay] = useState<DayOfWeek | null>(null)

  const [drafts, setDrafts] = useState<Record<DayOfWeek, { start: string; end: string }>>(
    () =>
      Object.fromEntries(DAYS.map((d) => [d.value, { start: '08:00', end: '22:00' }])) as Record<
        DayOfWeek,
        { start: string; end: string }
      >,
  )

  const [holidayDate, setHolidayDate] = useState('')
  const [holidayDescription, setHolidayDescription] = useState('')
  const [creatingHoliday, setCreatingHoliday] = useState(false)
  const [holidayError, setHolidayError] = useState<string | null>(null)

  async function reload() {
    setLoading(true)
    try {
      const [s, h] = await Promise.all([getGymSchedule(), getHolidays()])
      setSchedule(s)
      setHolidays(h)
      setDrafts((prev) => {
        const next = { ...prev }
        for (const entry of s) {
          next[entry.day] = { start: entry.openingTime.slice(0, 5), end: entry.closingTime.slice(0, 5) }
        }
        return next
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
  }, [])

  async function saveDay(day: DayOfWeek) {
    setSavingDay(day)
    try {
      const { start, end } = drafts[day]
      await upsertGymScheduleDay({ day, startTime: `${start}:00`, endTime: `${end}:00` })
      await reload()
    } finally {
      setSavingDay(null)
    }
  }

  async function handleCreateHoliday(e: FormEvent) {
    e.preventDefault()
    setCreatingHoliday(true)
    setHolidayError(null)
    try {
      await createHoliday({ date: holidayDate, description: holidayDescription })
      setHolidayDate('')
      setHolidayDescription('')
      await reload()
    } catch {
      setHolidayError('Dodavanje praznika nije uspelo - taj datum je možda već označen.')
    } finally {
      setCreatingHoliday(false)
    }
  }

  const scheduleByDay = Object.fromEntries(schedule.map((s) => [s.day, s]))

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-1 text-sm font-semibold text-slate-300">Radno vreme teretane</h3>
        <p className="mb-3 text-xs text-slate-500">
          Unos po danu je "upsert" - ponovno čuvanje istog dana ažurira postojeće radno vreme
          umesto da javi grešku (videti AGENTS.md "Upgrade: Faza 6 decisions").
        </p>
        {loading ? (
          <p className="text-sm text-slate-500">Učitavanje...</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-800 text-xs uppercase tracking-wide text-slate-500">
                  <th className="py-2 pr-4">Dan</th>
                  <th className="py-2 pr-4">Otvara</th>
                  <th className="py-2 pr-4">Zatvara</th>
                  <th className="py-2 pr-4">Trenutno</th>
                  <th className="py-2 pr-4" />
                </tr>
              </thead>
              <tbody>
                {DAYS.map((d) => {
                  const current = scheduleByDay[d.value]
                  return (
                    <tr key={d.value} className="border-b border-slate-800/60">
                      <td className="py-2 pr-4 text-slate-300">{d.label}</td>
                      <td className="py-2 pr-4">
                        <input
                          type="time"
                          value={drafts[d.value].start}
                          onChange={(e) =>
                            setDrafts((prev) => ({
                              ...prev,
                              [d.value]: { ...prev[d.value], start: e.target.value },
                            }))
                          }
                          className="rounded-lg border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100 outline-none focus:border-brand-500"
                        />
                      </td>
                      <td className="py-2 pr-4">
                        <input
                          type="time"
                          value={drafts[d.value].end}
                          onChange={(e) =>
                            setDrafts((prev) => ({
                              ...prev,
                              [d.value]: { ...prev[d.value], end: e.target.value },
                            }))
                          }
                          className="rounded-lg border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100 outline-none focus:border-brand-500"
                        />
                      </td>
                      <td className="py-2 pr-4 text-xs text-slate-500">
                        {current
                          ? `${current.openingTime.slice(0, 5)}–${current.closingTime.slice(0, 5)}`
                          : 'nije uneto'}
                      </td>
                      <td className="py-2 pr-4">
                        <button
                          onClick={() => saveDay(d.value)}
                          disabled={savingDay === d.value}
                          className="rounded-lg bg-brand-600 px-3 py-1 text-xs font-medium text-white hover:bg-brand-500 disabled:opacity-60"
                        >
                          {savingDay === d.value ? 'Čuvanje...' : 'Sačuvaj'}
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Praznici</h3>
        <form onSubmit={handleCreateHoliday} className="mb-4 flex flex-wrap items-end gap-3">
          <label className="block text-xs text-slate-400">
            Datum
            <input
              type="date"
              lang="sr-Latn-RS"
              required
              value={holidayDate}
              onChange={(e) => setHolidayDate(e.target.value)}
              className="mt-1 rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
          <label className="block text-xs text-slate-400">
            Opis
            <input
              type="text"
              required
              value={holidayDescription}
              onChange={(e) => setHolidayDescription(e.target.value)}
              placeholder="npr. Nova godina"
              className="mt-1 w-56 rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
          <button
            type="submit"
            disabled={creatingHoliday}
            className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-500 disabled:opacity-60"
          >
            {creatingHoliday ? 'Dodavanje...' : 'Dodaj praznik'}
          </button>
        </form>
        {holidayError && <p className="mb-3 text-xs text-red-400">{holidayError}</p>}

        {holidays.length === 0 ? (
          <p className="text-sm text-slate-500">Nema unetih praznika.</p>
        ) : (
          <ul className="space-y-1">
            {holidays.map((h) => (
              <li
                key={h.id}
                className="flex items-center justify-between rounded-lg bg-slate-950/60 px-3 py-2 text-sm text-slate-300"
              >
                <span>{h.date}</span>
                <span className="text-slate-400">{h.description}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
