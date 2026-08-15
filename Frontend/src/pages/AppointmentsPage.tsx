import { useEffect, useState } from 'react'
import { appointmentsApi } from '../api/appointments'
import { errorMessage } from '../api/client'
import { holidayApi, trainerScheduleApi } from '../api/schedules'
import { useAuthStore } from '../auth/authStore'
import type { AppointmentSummary, Holiday, TrainerSchedule } from '../types'
import { MonthCalendar } from '../components/MonthCalendar'

const future = (item: AppointmentSummary) => new Date(`${item.date}T${item.startTime}`) > new Date()
const dateLabel = (item: AppointmentSummary) => new Date(`${item.date}T12:00`).toLocaleDateString('sr-Latn-RS', { weekday: 'short', day: '2-digit', month: 'short' })

function AppointmentCard({ appointment, action, actionLabel, disabled }: { appointment: AppointmentSummary; action?: () => void; actionLabel?: string; disabled?: boolean }) {
  return <article className="appointment-card">
    <div className="appointment-date"><strong>{dateLabel(appointment)}</strong><time>{appointment.startTime.slice(0, 5)}–{appointment.endTime.slice(0, 5)}</time></div>
    <div><h3>{appointment.session.type === 'INDIVIDUAL' ? 'Individualni trening' : 'Grupni trening'}</h3><p>{appointment.trainer?.email ?? 'Trener još nije dodeljen'} · {appointment.room?.name ?? 'Sala nije dodeljena'}</p><small>{appointment.clients.length}/{appointment.session.maxParticipants} rezervisanih mesta</small></div>
    {action && <button className="secondary-button" disabled={disabled} onClick={action}>{actionLabel}</button>}
  </article>
}

export function AppointmentsPage() {
  const trainer = useAuthStore(s => s.session?.activeRole) === 'TRAINER'
  const [mine, setMine] = useState<AppointmentSummary[]>([])
  const [open, setOpen] = useState<AppointmentSummary[]>([])
  const [schedules, setSchedules] = useState<TrainerSchedule[]>([])
  const [holidays, setHolidays] = useState<Holiday[]>([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState<number | null>(null)
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().slice(0, 10))
  async function load() {
    try {
      const [owned, market, ownSchedules, gymHolidays] = await Promise.all([appointmentsApi.mine(), trainer ? appointmentsApi.withoutTrainer() : appointmentsApi.available(), trainer ? trainerScheduleApi.list() : Promise.resolve([]), trainer ? holidayApi.list() : Promise.resolve([])])
      setMine(owned)
      setOpen(market.filter(item => !owned.some(own => own.id === item.id)))
      setSchedules(ownSchedules)
      setHolidays(gymHolidays)
    } catch (cause) { setError(errorMessage(cause)) }
  }
  useEffect(() => { void load() }, [trainer])
  async function act(id: number, operation: () => Promise<unknown>) {
    setBusy(id); setError('')
    try { await operation(); await load() } catch (cause) { setError(errorMessage(cause)) } finally { setBusy(null) }
  }
  const selectedMine = mine.filter(item => item.date === selectedDate)
  const today = new Date().toISOString().slice(0, 10)
  const isPastDate = selectedDate < today
  const isFutureDate = selectedDate > today
  const upcoming = selectedMine.filter(future).sort((a, b) => a.date.localeCompare(b.date) || a.startTime.localeCompare(b.startTime))
  const history = selectedMine.filter(item => !future(item))
  const visibleOpen = open.filter(item => item.date === selectedDate)
  const mutedDates = new Set([...schedules.filter(row=>row.status!=='WORKING').map(row=>row.date),...holidays.map(holiday=>holiday.date)])
  return <main className="workspace-page appointments-page">
    <header className="workspace-header"><div><p className="eyebrow">{trainer ? 'Trenerski portal' : 'Klijentski portal'}</p><h1>Moji termini</h1><p>{trainer ? 'Pregledajte svoj angažman i preuzmite slobodne termine iz ponude.' : 'Rezervišite novi trening i pratite svoju istoriju dolazaka.'}</p></div></header>
    {error && <div className="content-error">{error}<button onClick={() => setError('')}>×</button></div>}
    <div className="calendar-list-layout"><MonthCalendar value={selectedDate} onChange={setSelectedDate} highlightedDates={new Set(mine.map(item=>item.date))} mutedDates={mutedDates}/><div>{!isPastDate&&<section className="appointment-section"><div className="card-head"><div><p className="eyebrow">Sledeće</p><h2>Predstojeći termini izabranog dana</h2></div><strong>{upcoming.length}</strong></div><div className="appointment-list">{upcoming.map(item => <AppointmentCard key={item.id} appointment={item} action={() => void act(item.id, () => trainer ? appointmentsApi.unassign(item.id) : appointmentsApi.cancel(item.id))} actionLabel={busy === item.id ? 'Otkazujem…' : trainer ? 'Otkaži dodelu' : 'Otkaži rezervaciju'} disabled={busy !== null || (!trainer && new Date(`${item.date}T${item.startTime}`).getTime() - Date.now() <= 86_400_000)} />)}{!upcoming.length && <div className="empty-panel">Nema budućih termina za izabrani datum.</div>}</div>{!trainer && <p className="appointment-note">Rezervaciju je moguće otkazati najkasnije 24 sata pre početka termina.</p>}</section>}
    {!isPastDate&&<section className="appointment-section marketplace"><div className="card-head"><div><p className="eyebrow">{trainer ? 'Marketplace' : 'Zakaži trening'}</p><h2>{trainer ? 'Termini bez trenera' : 'Dostupni termini'}</h2></div><strong>{visibleOpen.length}</strong></div><div className="appointment-list">{visibleOpen.map(item => <AppointmentCard key={item.id} appointment={item} action={() => void act(item.id, () => trainer ? appointmentsApi.assign(item.id) : appointmentsApi.reserve(item.id))} actionLabel={busy === item.id ? (trainer ? 'Preuzimam…' : 'Rezervišem…') : (trainer ? 'Preuzmi termin' : 'Rezerviši mesto')} disabled={busy !== null} />)}{!visibleOpen.length && <div className="empty-panel">Trenutno nema dostupnih termina za izabrani datum.</div>}</div></section>}
    {!isFutureDate&&<section className="appointment-section muted-section"><div className="card-head"><div><p className="eyebrow">Arhiva</p><h2>Održani treninzi izabranog dana</h2></div><strong>{history.length}</strong></div><div className="appointment-list compact">{history.map(item => <AppointmentCard key={item.id} appointment={item} />)}</div></section>}</div></div>
  </main>
}
