import { useEffect, useState } from 'react'
import { appointmentsApi } from '../api/appointments'
import { errorMessage } from '../api/client'
import { MonthCalendar } from '../components/MonthCalendar'
import type { AppointmentSummary } from '../types'
import { gymScheduleApi, holidayApi } from '../api/schedules'
import { gymClosure } from '../utils/gymAvailability'

const future=(item:AppointmentSummary)=>new Date(`${item.date}T${item.startTime}`)>new Date()
const dateLabel=(item:AppointmentSummary)=>new Date(`${item.date}T12:00`).toLocaleDateString('sr-Latn-RS',{weekday:'short',day:'2-digit',month:'short'})

function ReservedAppointment({appointment}:{appointment:AppointmentSummary}){
  return <article className="appointment-card"><div className="appointment-date"><strong>{dateLabel(appointment)}</strong><time>{appointment.startTime.slice(0,5)}–{appointment.endTime.slice(0,5)}</time></div><div><h3>{appointment.session.type==='INDIVIDUAL'?'Individualni trening':'Grupni trening'}</h3><p>{appointment.trainer?.email??'Trener još nije dodeljen'} · {appointment.room?.name??'Sala nije dodeljena'}</p><small>{appointment.clients.length}/{appointment.session.maxParticipants} rezervisanih mesta</small></div></article>
}

export function ClientAppointmentsPage(){
  const[selectedDate,setSelectedDate]=useState(new Date().toISOString().slice(0,10))
  const[appointments,setAppointments]=useState<AppointmentSummary[]>([])
  const[error,setError]=useState('')
  const[closure,setClosure]=useState({mutedDates:new Set<string>(),mutedWeekdays:new Set<number>()})
  useEffect(()=>{Promise.all([appointmentsApi.mine(),gymScheduleApi.list(),holidayApi.list()]).then(([rows,schedules,holidays])=>{setAppointments(rows);setClosure(gymClosure(schedules,holidays))}).catch(cause=>setError(errorMessage(cause)))},[])
  const today=new Date().toISOString().slice(0,10)
  const isPastDate=selectedDate<today
  const isFutureDate=selectedDate>today
  const selected=appointments.filter(item=>item.date===selectedDate)
  const upcoming=selected.filter(future).sort((a,b)=>a.startTime.localeCompare(b.startTime))
  const history=selected.filter(item=>!future(item)).sort((a,b)=>b.startTime.localeCompare(a.startTime))
  return <main className="workspace-page appointments-page">
    <header className="workspace-header"><div><p className="eyebrow">Klijentski portal</p><h1>Moji termini</h1><p>Pregledajte samo treninge na koje ste već rezervisani.</p></div></header>
    {error&&<div className="content-error">{error}<button onClick={()=>setError('')}>×</button></div>}
    <div className="calendar-list-layout"><MonthCalendar value={selectedDate} onChange={setSelectedDate} highlightedDates={new Set(appointments.map(item=>item.date))} mutedDates={closure.mutedDates} mutedWeekdays={closure.mutedWeekdays}/><div>
      {!isPastDate&&<section className="appointment-section"><div className="card-head"><div><p className="eyebrow">Sledeće</p><h2>Predstojeći termini izabranog dana</h2></div><strong>{upcoming.length}</strong></div><div className="appointment-list">{upcoming.map(item=><ReservedAppointment key={item.id} appointment={item}/>)}{!upcoming.length&&<div className="empty-panel">Nema predstojećih rezervisanih termina za izabrani dan.</div>}</div></section>}
      {!isFutureDate&&<section className="appointment-section muted-section"><div className="card-head"><div><p className="eyebrow">Arhiva</p><h2>Održani treninzi izabranog dana</h2></div><strong>{history.length}</strong></div><div className="appointment-list compact">{history.map(item=><ReservedAppointment key={item.id} appointment={item}/>)}{!history.length&&<div className="empty-panel">Nema održanih treninga za izabrani dan.</div>}</div></section>}
    </div></div>
  </main>
}
