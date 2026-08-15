import { useEffect, useState } from 'react'
import { clientsApi } from '../api/administration'
import { errorMessage } from '../api/client'
import { paymentsApi } from '../api/operations'
import { useAuthStore } from '../auth/authStore'
import { PaymentStatusSummary } from '../components/PaymentStatusSummary'
import type { ClientProfile, Payment, PaymentStatus } from '../types'

export function PaymentsPage(){
  const manager=useAuthStore(s=>s.session?.activeRole)==='MANAGER'
  const[payments,setPayments]=useState<Payment[]>([])
  const[statuses,setStatuses]=useState<PaymentStatus[]>([])
  const[clients,setClients]=useState<ClientProfile[]>([])
  const[filter,setFilter]=useState('')
  const[form,setForm]=useState({clientId:0,sessionId:1,paidAppointments:10,paymentDate:new Date().toISOString().slice(0,10)})
  const[error,setError]=useState('')
  const[busy,setBusy]=useState(false)
  async function load(){
    try{
      setPayments(manager?await paymentsApi.list(filter?Number(filter):undefined):await paymentsApi.mine())
      setStatuses(manager?(filter?await paymentsApi.status(Number(filter)):[]):await paymentsApi.myStatus())
    }catch(cause){setError(errorMessage(cause))}
  }
  useEffect(()=>{if(manager)clientsApi.list().then(data=>{setClients(data);if(data[0])setForm(value=>({...value,clientId:data[0].id}))}).catch(cause=>setError(errorMessage(cause)));void load()},[manager])
  useEffect(()=>{if(manager)void load()},[filter])
  async function submit(event:React.FormEvent){event.preventDefault();setBusy(true);setError('');try{await paymentsApi.create(form);await load()}catch(cause){setError(errorMessage(cause))}finally{setBusy(false)}}
  return <main className="workspace-page payments-page">
    <header className="workspace-header"><div><p className="eyebrow">{manager?'Manager / finansije':'Klijentski portal'}</p><h1>{manager?'Plaćanja':'Moje uplate'}</h1><p>{manager?'Evidencija kupljenih termina po klijentu i tipu treninga.':'Istorija vaših paketa i status iskorišćenih termina.'}</p></div>{manager&&<select className="client-picker" value={filter} onChange={event=>setFilter(event.target.value)}><option value="">Svi klijenti</option>{clients.map(client=><option key={client.id} value={client.id}>{client.user.email}</option>)}</select>}</header>
    {error&&<div className="content-error">{error}<button onClick={()=>setError('')}>×</button></div>}
    {(!manager||filter)&&<PaymentStatusSummary statuses={statuses}/>}
    <div className={manager?'payments-layout':''}>{manager&&<form className="progress-card payment-form" onSubmit={submit}><p className="eyebrow">Nova uplata</p><h2>Evidentiraj paket</h2><label>Klijent<select required value={form.clientId||''} onChange={event=>setForm({...form,clientId:Number(event.target.value)})}>{clients.map(client=><option key={client.id} value={client.id}>{client.user.email}</option>)}</select></label><label>Tip termina<select value={form.sessionId} onChange={event=>setForm({...form,sessionId:Number(event.target.value)})}><option value={1}>Individualni · 1</option><option value={2}>Grupni · 3</option><option value={3}>Grupni · 10</option></select></label><label>Broj termina<input required min="1" type="number" value={form.paidAppointments} onChange={event=>setForm({...form,paidAppointments:Number(event.target.value)})}/></label><label>Datum uplate<input required type="date" value={form.paymentDate} onChange={event=>setForm({...form,paymentDate:event.target.value})}/></label><button className="primary-button" disabled={busy||!clients.length}>{busy?'Čuvam…':'Sačuvaj uplatu'}</button></form>}
      <section className="progress-card table-card"><div className="card-head"><div><p className="eyebrow">Istorija</p><h2>{payments.length} evidentiranih uplata</h2></div></div><div className="payment-list">{payments.map(payment=><article key={payment.id}><time>{new Date(payment.paymentDate+'T12:00').toLocaleDateString('sr-RS')}</time><div><strong>{payment.client.email}</strong><small>{payment.session.type==='INDIVIDUAL'?'Individualni':'Grupni'} trening · kapacitet {payment.session.maxParticipants}</small></div><b>{payment.paidAppointments}<small> termina</small></b></article>)}{!payments.length&&<div className="empty-panel">Još nema evidentiranih uplata.</div>}</div></section>
    </div>
  </main>
}
