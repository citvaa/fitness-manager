import { type FormEvent, useEffect, useMemo, useState } from 'react'
import { isAxiosError } from 'axios'
import { DateInput } from '../../components/DateInput'
import { LoadingIndicator } from '../../components/LoadingIndicator'
import { MonthCalendar } from '../../components/MonthCalendar'
import { useConfirm } from '../../components/ConfirmDialog'
import { buildGymMutedReason } from '../../lib/gymAvailability'
import {
  createMySchedule,
  createMyScheduleRecurring,
  createMyUnavailability,
  deleteMyScheduleEntry,
  getMyAppointmentsForScheduleCheck,
  getMySchedule,
} from './api'
import type { MyAppointmentSlimDTO, TrainerScheduleDTO, WorkStatus } from './types'

/**
 * The backend now returns a real, specific message for validation errors (see AGENTS.md
 * "Known issues"/GlobalExceptionHandler - "No gym schedule found for ...", "Trainer already has
 * a shift overlapping...", etc.) instead of a bare 500. Prefer that message; fall back to a
 * generic Serbian one only if the response has no body (network error, unexpected shape).
 */
function extractErrorMessage(err: unknown, fallback: string): string {
  if (isAxiosError(err) && typeof err.response?.data?.message === 'string') {
    return err.response.data.message
  }
  return fallback
}

/** Same multi-line-message rendering as AppointmentsTab.tsx's local ErrorMessage - the recurring
 * schedule endpoint's "nothing could be created" message is a per-date breakdown, same convention
 * as AppointmentService#createRecurringWeekly. See AGENTS.md. */
function ErrorMessage({ message, className }: { message: string; className: string }) {
  const lines = message.split('\n').filter((line) => line.trim().length > 0)
  if (lines.length <= 1) {
    return <p className={className}>{message}</p>
  }
  const [heading, ...rest] = lines
  return (
    <div className={className}>
      <p>{heading}</p>
      <ul className="mt-1 list-disc space-y-1 pl-4">
        {rest.map((line, i) => (
          <li key={i}>{line}</li>
        ))}
      </ul>
    </div>
  )
}

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

/** Whether some WORKING entry on the appointment's own date fully covers its time range - the
 * same coverage rule AppointmentServiceImpl#isTrainerAvailable enforces server-side at create
 * time. Purely a client-side display aid (B2) - it does not call the backend per-appointment,
 * just re-derives the same rule from data already fetched for this page. See AGENTS.md "Upgrade:
 * trainer fixed-schedule decisions". */
function isCoveredByWorkingSchedule(entries: TrainerScheduleDTO[], apt: MyAppointmentSlimDTO): boolean {
  return entries.some(
    (e) =>
      e.status === 'WORKING' &&
      e.date === apt.date &&
      e.startTime <= apt.startTime &&
      e.endTime >= apt.endTime,
  )
}

/**
 * TRAINER self-service - a trainer enters/manages only their own working hours and
 * unavailability, scoped server-side to the trainer resolved from the JWT (never a client-
 * supplied trainerId). See AGENTS.md "Upgrade: Faza 6 decisions".
 *
 * Restructured (see AGENTS.md "Upgrade: trainer fixed-schedule decisions"):
 * - "Fiksni raspored" checkbox generates weekly-repeating WORKING shifts (B1), same 8-week
 *   convention as the manager's "fiksni termin" for appointments.
 * - "Moji uneti termini" is now a MonthCalendar day-picker instead of a flat list (B3), same
 *   component as the admin Termini tab / manager dnevni raspored.
 * - Selecting a day also cross-references the trainer's own MANAGER-assigned appointments on
 *   that date against the WORKING shifts shown, flagging any appointment that isn't actually
 *   covered by a current shift (B2) - so a trainer editing their fixed schedule doesn't have to
 *   separately open "Moji termini" to notice they'd be leaving an already-assigned appointment
 *   unstaffed.
 */
export function TrainerSchedulePage() {
  const [entries, setEntries] = useState<TrainerScheduleDTO[]>([])
  const [appointments, setAppointments] = useState<MyAppointmentSlimDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedDate, setSelectedDate] = useState(todayIso())

  const [date, setDate] = useState('')
  const [startTime, setStartTime] = useState('09:00')
  const [endTime, setEndTime] = useState('17:00')
  const [recurring, setRecurring] = useState(false)
  const [savingShift, setSavingShift] = useState(false)
  const [createInfo, setCreateInfo] = useState<string | null>(null)

  const [unavailStart, setUnavailStart] = useState('')
  const [unavailEnd, setUnavailEnd] = useState('')
  const [unavailStatus, setUnavailStatus] = useState<WorkStatus>('VACATION')
  const [savingUnavail, setSavingUnavail] = useState(false)
  const confirm = useConfirm()
  const [gymMutedReason, setGymMutedReason] = useState<((iso: string) => string | null) | null>(null)

  useEffect(() => {
    void buildGymMutedReason().then(setGymMutedReason)
  }, [])

  async function reload() {
    setLoading(true)
    try {
      const [schedule, myAppointments] = await Promise.all([getMySchedule(), getMyAppointmentsForScheduleCheck()])
      setEntries(schedule)
      setAppointments(myAppointments)
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
    setCreateInfo(null)
    setSavingShift(true)
    try {
      if (recurring) {
        const created = await createMyScheduleRecurring({ date, startTime: `${startTime}:00`, endTime: `${endTime}:00` })
        setCreateInfo(`Kreirano ${created.length} instanci fiksnog rasporeda.`)
      } else {
        await createMySchedule({ date, startTime: `${startTime}:00`, endTime: `${endTime}:00` })
      }
      setDate('')
      await reload()
    } catch (err) {
      setError(
        extractErrorMessage(
          err,
          'Unos radnog vremena nije uspeo - proveri da li je datum u okviru radnog vremena teretane i da se ne preklapa sa postojećom smenom.',
        ),
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
    } catch (err) {
      setError(extractErrorMessage(err, 'Unos neradnog perioda nije uspeo.'))
    } finally {
      setSavingUnavail(false)
    }
  }

  async function handleDelete(id: number) {
    if (!(await confirm('Obrisati ovaj unos rasporeda?'))) return
    await deleteMyScheduleEntry(id)
    await reload()
  }

  const highlightedDates = useMemo(() => new Set(entries.map((e) => e.date)), [entries])

  // Gym-wide closure (holiday/no weekly hours) plus this trainer's own non-WORKING entries
  // (HOLIDAY/SICK_LEAVE/VACATION) - a trainer editing their schedule should see at a glance which
  // days they've already marked unavailable. See AGENTS.md "Upgrade: MonthCalendar unavailability
  // decisions".
  const getMutedReason = useMemo(() => {
    const nonWorkingByDate = new Map(
      entries.filter((e) => e.status !== 'WORKING').map((e) => [e.date, STATUS_LABEL[e.status]]),
    )
    return (iso: string) => {
      const own = nonWorkingByDate.get(iso)
      if (own) return `Nedostupnost: ${own}`
      return gymMutedReason?.(iso) ?? null
    }
  }, [entries, gymMutedReason])

  const entriesForDate = useMemo(
    () => entries.filter((e) => e.date === selectedDate).sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [entries, selectedDate],
  )
  const appointmentsForDate = useMemo(
    () => appointments.filter((a) => a.date === selectedDate).sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [appointments, selectedDate],
  )

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
          <label className="mb-3 flex items-center gap-2 text-xs text-slate-300">
            <input
              type="checkbox"
              checked={recurring}
              onChange={(e) => setRecurring(e.target.checked)}
              className="h-4 w-4 rounded border-slate-700 bg-slate-950 text-brand-600 focus:ring-brand-500"
            />
            Fiksni raspored (ponavlja se svake nedelje na isti dan i u isto vreme)
          </label>
          <div className="space-y-3">
            <label className="block text-xs text-slate-400">
              {recurring ? 'Datum prve smene' : 'Datum'}
              <DateInput
                required
                value={date}
                onChange={setDate}
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
          <p className="mt-3 text-xs text-slate-500">
            {recurring && 'Fiksni raspored generiše smene nedeljno unapred (nekoliko meseci); pokreni ponovo da produžiš niz.'}
          </p>
          <button
            type="submit"
            disabled={savingShift}
            className="mt-4 w-full rounded-lg bg-brand-600 px-3 py-2 text-sm font-medium text-white hover:bg-brand-500 disabled:opacity-60"
          >
            {savingShift ? 'Čuvanje...' : recurring ? 'Kreiraj fiksni raspored' : 'Dodaj smenu'}
          </button>
          {createInfo && <p className="mt-3 text-xs text-emerald-400">{createInfo}</p>}
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
                <DateInput
                  required
                  value={unavailStart}
                  onChange={setUnavailStart}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
                />
              </label>
              <label className="block flex-1 text-xs text-slate-400">
                Do datuma
                <DateInput
                  required
                  value={unavailEnd}
                  onChange={setUnavailEnd}
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

      {error && <ErrorMessage message={error} className="mt-4 rounded-lg bg-red-950/60 px-3 py-2 text-sm text-red-300" />}

      <div className="mt-6 grid gap-4 lg:grid-cols-[auto,1fr]">
        <MonthCalendar
          value={selectedDate}
          onChange={setSelectedDate}
          highlightedDates={highlightedDates}
          getMutedReason={getMutedReason}
        />

        <div className="space-y-4">
          <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
            <h3 className="mb-3 text-sm font-semibold text-slate-300">Raspored za {selectedDate}</h3>
            {loading ? (
              <LoadingIndicator className="text-sm text-slate-500" />
            ) : entriesForDate.length === 0 ? (
              <p className="text-sm text-slate-500">Ništa uneto za ovaj dan.</p>
            ) : (
              <ul className="space-y-1 text-sm">
                {entriesForDate.map((e) => (
                  <li
                    key={e.id}
                    className="flex items-center justify-between rounded-lg bg-slate-950/60 px-3 py-2"
                  >
                    <span className="text-slate-300">
                      {e.startTime.slice(0, 5)}–{e.endTime.slice(0, 5)} ·{' '}
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

          <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
            <h3 className="mb-1 text-sm font-semibold text-slate-300">
              Termini dodeljeni od menadžera za {selectedDate}
            </h3>
            <p className="mb-3 text-xs text-slate-500">
              Da li se poklapaju sa radnim vremenom iznad - ako termin nije pokriven trenutnom smenom, dopuni raspored.
            </p>
            {loading ? (
              <LoadingIndicator className="text-sm text-slate-500" />
            ) : appointmentsForDate.length === 0 ? (
              <p className="text-sm text-slate-500">Nema dodeljenih termina za ovaj dan.</p>
            ) : (
              <ul className="space-y-1 text-sm">
                {appointmentsForDate.map((a) => {
                  const covered = isCoveredByWorkingSchedule(entries, a)
                  return (
                    <li
                      key={a.id}
                      className="flex items-center justify-between rounded-lg bg-slate-950/60 px-3 py-2"
                    >
                      <span className="text-slate-300">
                        {a.startTime.slice(0, 5)}–{a.endTime.slice(0, 5)}
                      </span>
                      {covered ? (
                        <span className="text-xs text-emerald-400">✓ pokriven rasporedom</span>
                      ) : (
                        <span className="text-xs text-amber-400">⚠ nije pokriven trenutnim rasporedom</span>
                      )}
                    </li>
                  )
                })}
              </ul>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
