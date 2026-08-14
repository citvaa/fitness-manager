import { type FormEvent, useEffect, useMemo, useState } from 'react'
import { isAxiosError } from 'axios'
import { MonthCalendar } from '../../components/MonthCalendar'
import {
  addClientToAppointment,
  assignTrainerToAppointment,
  createAppointment,
  createRecurringAppointment,
  getAllAppointments,
  getClients,
  getRoomsForPicker,
  getSessionsForPicker,
  getTrainers,
  removeClientFromAppointment,
  removeTrainerFromAppointment,
} from './api'
import type { AppointmentDTO, ClientDTO, RoomOptionDTO, SessionDTO, TrainerDTO } from './types'

/** See the same helper in features/schedule/TrainerSchedulePage.tsx - the backend now returns a
 * real, specific validation message instead of a bare 500 (GlobalExceptionHandler). Appointment
 * creation runs the same gym-hours/trainer-overlap/client-overlap validation as the seeder's data,
 * so a real message here (not a generic fallback) matters just as much. */
function extractErrorMessage(err: unknown, fallback: string): string {
  if (isAxiosError(err) && typeof err.response?.data?.message === 'string') {
    return err.response.data.message
  }
  return fallback
}

const SESSION_TYPE_LABEL: Record<string, string> = { INDIVIDUAL: 'Individualni', GROUP: 'Grupni' }

function todayIso(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

const EMPTY_FORM = {
  date: '',
  startTime: '09:00',
  endTime: '10:00',
  sessionId: '',
  trainerId: '',
  roomId: '',
  recurring: false,
}

/**
 * MANAGER slot management - the last missing piece of the Faza 7 "marketplace" model. Create/
 * assign-trainer/remove-trainer/add-client/remove-client all existed on the backend since
 * Faza 2/7 with zero frontend caller - without this screen every appointment in the app only
 * ever came from the dev seeder. See AGENTS.md ("Upgrade: Faza 9 decisions").
 *
 * Restructured in manager-testing round 3 (see AGENTS.md "Upgrade: fixed weekly appointment
 * decisions"): trainer/room are now required, a "fiksni termin" option generates weekly-
 * recurring instances, and the previous flat scrollable list of every appointment is replaced by
 * a calendar day-picker (only that day's appointments render) plus trainer/room/session filters.
 */
export function AppointmentsTab() {
  const [appointments, setAppointments] = useState<AppointmentDTO[]>([])
  const [sessions, setSessions] = useState<SessionDTO[]>([])
  const [trainers, setTrainers] = useState<TrainerDTO[]>([])
  const [clients, setClients] = useState<ClientDTO[]>([])
  const [rooms, setRooms] = useState<RoomOptionDTO[]>([])
  const [loading, setLoading] = useState(true)

  const [selectedDate, setSelectedDate] = useState(todayIso())
  const [filterTrainerId, setFilterTrainerId] = useState('')
  const [filterRoomId, setFilterRoomId] = useState('')
  const [filterSessionType, setFilterSessionType] = useState('')

  const [form, setForm] = useState(EMPTY_FORM)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [createInfo, setCreateInfo] = useState<string | null>(null)

  const [rowError, setRowError] = useState<string | null>(null)
  const [addClientChoice, setAddClientChoice] = useState<Record<number, string>>({})

  async function reload() {
    setLoading(true)
    try {
      const [a, s, t, c, r] = await Promise.all([
        getAllAppointments(),
        getSessionsForPicker(),
        getTrainers(),
        getClients(),
        getRoomsForPicker(),
      ])
      setAppointments(a)
      setSessions(s)
      setTrainers(t)
      setClients(c)
      setRooms(r)
      if (s.length > 0 && !form.sessionId) {
        setForm((f) => ({ ...f, sessionId: String(s[0].id) }))
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function handleCreate(e: FormEvent) {
    e.preventDefault()
    setCreating(true)
    setCreateError(null)
    setCreateInfo(null)
    const request = {
      date: form.date,
      startTime: `${form.startTime}:00`,
      endTime: `${form.endTime}:00`,
      sessionId: Number(form.sessionId),
      trainerId: Number(form.trainerId),
      roomId: Number(form.roomId),
    }
    try {
      if (form.recurring) {
        const created = await createRecurringAppointment({ ...request, recurring: true })
        setCreateInfo(`Kreirano ${created.length} instanci fiksnog termina.`)
      } else {
        await createAppointment(request)
      }
      setForm((f) => ({ ...EMPTY_FORM, sessionId: f.sessionId }))
      await reload()
    } catch (err) {
      setCreateError(
        extractErrorMessage(
          err,
          'Kreiranje termina nije uspelo - proveri da li je u okviru radnog vremena teretane i da se trener/klijenti/soba ne preklapaju sa drugim terminom.',
        ),
      )
    } finally {
      setCreating(false)
    }
  }

  async function handleAssignTrainer(appointmentId: number, trainerId: string) {
    if (!trainerId) return
    setRowError(null)
    try {
      await assignTrainerToAppointment(appointmentId, Number(trainerId))
      await reload()
    } catch (err) {
      setRowError(extractErrorMessage(err, 'Dodela trenera nije uspela.'))
    }
  }

  async function handleRemoveTrainer(appointmentId: number) {
    setRowError(null)
    try {
      await removeTrainerFromAppointment(appointmentId)
      await reload()
    } catch (err) {
      setRowError(extractErrorMessage(err, 'Uklanjanje trenera nije uspelo.'))
    }
  }

  async function handleAddClient(appointmentId: number) {
    const clientId = addClientChoice[appointmentId]
    if (!clientId) return
    setRowError(null)
    try {
      await addClientToAppointment(appointmentId, Number(clientId))
      setAddClientChoice((prev) => ({ ...prev, [appointmentId]: '' }))
      await reload()
    } catch (err) {
      setRowError(extractErrorMessage(err, 'Dodavanje klijenta nije uspelo.'))
    }
  }

  async function handleRemoveClient(appointmentId: number, clientId: number) {
    setRowError(null)
    try {
      await removeClientFromAppointment(appointmentId, clientId)
      await reload()
    } catch (err) {
      setRowError(extractErrorMessage(err, 'Uklanjanje klijenta nije uspelo.'))
    }
  }

  const highlightedDates = useMemo(() => new Set(appointments.map((a) => a.date)), [appointments])

  const visible = appointments
    .filter((a) => a.date === selectedDate)
    .filter((a) => !filterTrainerId || String(a.trainer?.id ?? '') === filterTrainerId)
    .filter((a) => !filterRoomId || String(a.room?.id ?? '') === filterRoomId)
    .filter((a) => !filterSessionType || a.session.type === filterSessionType)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))

  return (
    <div className="space-y-6">
      <form
        onSubmit={handleCreate}
        className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4"
      >
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Novi termin</h3>
        <label className="mb-3 flex items-center gap-2 text-xs text-slate-300">
          <input
            type="checkbox"
            checked={form.recurring}
            onChange={(e) => setForm((f) => ({ ...f, recurring: e.target.checked }))}
            className="h-4 w-4 rounded border-slate-700 bg-slate-950 text-brand-600 focus:ring-brand-500"
          />
          Fiksni termin (ponavlja se svake nedelje na isti dan i u isto vreme)
        </label>
        <div className="grid gap-3 md:grid-cols-3 lg:grid-cols-6">
          <label className="block text-xs text-slate-400">
            {form.recurring ? 'Datum prvog termina' : 'Datum'}
            <input
              type="date"
              lang="sr-Latn-RS"
              required
              value={form.date}
              onChange={(e) => setForm((f) => ({ ...f, date: e.target.value }))}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
          <label className="block text-xs text-slate-400">
            Početak
            <input
              type="time"
              required
              value={form.startTime}
              onChange={(e) => setForm((f) => ({ ...f, startTime: e.target.value }))}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
          <label className="block text-xs text-slate-400">
            Kraj
            <input
              type="time"
              required
              value={form.endTime}
              onChange={(e) => setForm((f) => ({ ...f, endTime: e.target.value }))}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
          <label className="block text-xs text-slate-400">
            Tip sesije
            <select
              required
              value={form.sessionId}
              onChange={(e) => setForm((f) => ({ ...f, sessionId: e.target.value }))}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            >
              <option value="" disabled>
                Izaberi...
              </option>
              {sessions.map((s) => (
                <option key={s.id} value={s.id}>
                  {SESSION_TYPE_LABEL[s.type] ?? s.type} (max {s.maxParticipants})
                </option>
              ))}
            </select>
          </label>
          <label className="block text-xs text-slate-400">
            Trener
            <select
              required
              value={form.trainerId}
              onChange={(e) => setForm((f) => ({ ...f, trainerId: e.target.value }))}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            >
              <option value="" disabled>
                Izaberi trenera...
              </option>
              {trainers.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.user.email}
                </option>
              ))}
            </select>
          </label>
          <label className="block text-xs text-slate-400">
            Soba
            <select
              required
              value={form.roomId}
              onChange={(e) => setForm((f) => ({ ...f, roomId: e.target.value }))}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            >
              <option value="" disabled>
                Izaberi sobu...
              </option>
              {rooms.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
          </label>
        </div>
        <p className="mt-3 text-xs text-slate-500">
          Klijenti se mogu dodati/ukloniti i posle kreiranja termina, u listi ispod.
          {form.recurring && ' Fiksni termin generiše instance nedeljno unapred (nekoliko meseci); pokreni ponovo da produžiš niz.'}
        </p>
        <button
          type="submit"
          disabled={creating}
          className="mt-3 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-500 disabled:opacity-60"
        >
          {creating ? 'Kreiranje...' : form.recurring ? 'Kreiraj fiksni termin' : 'Kreiraj termin'}
        </button>
        {createError && <p className="mt-3 text-xs text-red-400">{createError}</p>}
        {createInfo && <p className="mt-3 text-xs text-emerald-400">{createInfo}</p>}
      </form>

      <div className="grid gap-4 lg:grid-cols-[auto,1fr]">
        <MonthCalendar value={selectedDate} onChange={setSelectedDate} highlightedDates={highlightedDates} />

        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h3 className="text-sm font-semibold text-slate-300">
              Termini za {selectedDate} ({visible.length})
            </h3>
          </div>

          <div className="mb-3 grid gap-2 sm:grid-cols-3">
            <select
              value={filterTrainerId}
              onChange={(e) => setFilterTrainerId(e.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-xs text-slate-100 outline-none focus:border-brand-500"
            >
              <option value="">Svi treneri</option>
              {trainers.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.user.email}
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

          {rowError && <p className="mb-3 text-xs text-red-400">{rowError}</p>}

          {loading ? (
            <p className="text-sm text-slate-500">Učitavanje...</p>
          ) : visible.length === 0 ? (
            <p className="text-sm text-slate-500">Nema termina za prikaz na izabrani dan/filtere.</p>
          ) : (
            <ul className="max-h-[32rem] space-y-2 overflow-y-auto">
              {visible.map((a) => {
                const full = a.clients.length >= a.session.maxParticipants
                const availableClients = clients.filter(
                  (c) => !a.clients.some((existing) => existing.id === c.id),
                )
                return (
                  <li key={a.id} className="rounded-xl border border-slate-800 bg-slate-950/60 p-3 text-sm">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span className="font-medium text-slate-100">
                        {a.startTime.slice(0, 5)}–{a.endTime.slice(0, 5)}
                      </span>
                      <span className="text-xs text-slate-500">
                        {SESSION_TYPE_LABEL[a.session.type] ?? a.session.type} · {a.clients.length}/
                        {a.session.maxParticipants}
                        {full && <span className="ml-1 text-amber-400">(popunjeno)</span>}
                      </span>
                    </div>

                    <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-slate-400">
                      <span>Trener: {a.trainer?.email ?? 'nedodeljen'}</span>
                      {a.trainer ? (
                        <button
                          onClick={() => handleRemoveTrainer(a.id)}
                          className="rounded-lg border border-red-900/50 px-2 py-0.5 text-xs text-red-300 hover:bg-red-950/40"
                        >
                          Ukloni trenera
                        </button>
                      ) : (
                        <select
                          defaultValue=""
                          onChange={(e) => handleAssignTrainer(a.id, e.target.value)}
                          className="rounded-lg border border-slate-700 bg-slate-900 px-2 py-1 text-xs text-slate-100 outline-none focus:border-brand-500"
                        >
                          <option value="" disabled>
                            Dodeli trenera...
                          </option>
                          {trainers.map((t) => (
                            <option key={t.id} value={t.id}>
                              {t.user.email}
                            </option>
                          ))}
                        </select>
                      )}
                      <span className="text-slate-600">·</span>
                      <span>Soba: {a.room?.name ?? 'bez sobe'}</span>
                    </div>

                    <div className="mt-2 text-xs text-slate-400">
                      <div className="mb-1">Klijenti:</div>
                      {a.clients.length === 0 ? (
                        <p className="text-slate-600">nema prijavljenih</p>
                      ) : (
                        <ul className="mb-2 space-y-1">
                          {a.clients.map((c) => (
                            <li key={c.id} className="flex items-center justify-between rounded-lg bg-slate-900/60 px-2 py-1">
                              <span>{c.email}</span>
                              <button
                                onClick={() => handleRemoveClient(a.id, c.id)}
                                className="rounded-lg border border-red-900/50 px-2 py-0.5 text-xs text-red-300 hover:bg-red-950/40"
                              >
                                Ukloni
                              </button>
                            </li>
                          ))}
                        </ul>
                      )}
                      {!full && availableClients.length > 0 && (
                        <div className="flex items-center gap-2">
                          <select
                            value={addClientChoice[a.id] ?? ''}
                            onChange={(e) =>
                              setAddClientChoice((prev) => ({ ...prev, [a.id]: e.target.value }))
                            }
                            className="rounded-lg border border-slate-700 bg-slate-900 px-2 py-1 text-xs text-slate-100 outline-none focus:border-brand-500"
                          >
                            <option value="">Dodaj klijenta...</option>
                            {availableClients.map((c) => (
                              <option key={c.id} value={c.id}>
                                {c.user.email}
                              </option>
                            ))}
                          </select>
                          <button
                            onClick={() => handleAddClient(a.id)}
                            disabled={!addClientChoice[a.id]}
                            className="rounded-lg bg-brand-600 px-2 py-1 text-xs text-white hover:bg-brand-500 disabled:opacity-60"
                          >
                            Dodaj
                          </button>
                        </div>
                      )}
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}
