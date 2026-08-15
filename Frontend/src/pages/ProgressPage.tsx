import { useEffect, useState } from 'react'
import { errorMessage } from '../api/client'
import { progressApi, type PersonalRecordInput, type ProgressEntryInput } from '../api/progress'
import { useAuthStore } from '../auth/authStore'
import { PersonalRecordChart, ProgressChart } from '../components/ProgressChart'
import { useConfirm } from '../components/ConfirmDialog'
import type { AiInsight, ClientSummary, PersonalRecord, ProgressEntry, RecordUnit } from '../types'

const today=()=>new Date().toISOString().slice(0,10)
const measurements=[['weightKg','Težina','kg'],['bodyFatPercent','Masti','%'],['waistCm','Struk','cm'],['chestCm','Grudi','cm'],['hipCm','Kukovi','cm'],['thighCm','Butina','cm'],['armCm','Ruka','cm']] as const

function NarrativeText({text}:{text:string}) {
  const blocks=text.trim().split(/\r?\n\s*\r?\n/).filter(Boolean)
  if(blocks.length>=2)return <div className="narrative-sections">
    <section><span>Sažetak</span><p>{blocks[0].replace(/^[-•]\s*/gm,'')}</p></section>
    <section className="narrative-recommendation"><span>Preporuka</span><p>{blocks.slice(1).join('\n\n').replace(/^[-•]\s*/gm,'')}</p></section>
  </div>
  return <div className="narrative-copy">{text.split(/\n+/).filter(Boolean).map((line,index)=><p key={index}>{line.replace(/^[-•]\s*/,'')}</p>)}</div>
}

function Narrative({insight,loading,error,onRefresh,trainer}:{insight:AiInsight|null;loading:boolean;error:string;onRefresh:()=>void;trainer:boolean}) {
  return <article className="progress-card narrative-card">
    <div className="card-head"><div><p className="eyebrow">Claude trener</p><h2>Rezime i preporuka</h2></div>{trainer&&<button className="secondary-button" disabled={loading} onClick={onRefresh}>↻ Regeneriši</button>}</div>
    {error&&<div className="content-error">{error}</div>}
    {loading&&!insight?<div className="insight-skeleton dark"><i/><i/><i/></div>:insight?<><NarrativeText text={insight.text}/><small>Generisano {new Date(insight.generatedAt).toLocaleString('sr-RS')} · {insight.model}</small></>:!error&&<div className="empty-panel">Dodajte merenje ili rekord da bi AI mogao da napravi rezime.</div>}
  </article>
}

function EntryForm({clientId,onSaved,editing,onCancel}:{clientId:number;onSaved:()=>void;editing:ProgressEntry|null;onCancel:()=>void}) {
  const blank=():ProgressEntryInput=>({entryDate:today()})
  const[form,setForm]=useState<ProgressEntryInput>(blank())
  const[busy,setBusy]=useState(false)
  const[error,setError]=useState('')
  useEffect(()=>setForm(editing?{entryDate:editing.entryDate,weightKg:editing.weightKg??undefined,bodyFatPercent:editing.bodyFatPercent??undefined,waistCm:editing.waistCm??undefined,chestCm:editing.chestCm??undefined,hipCm:editing.hipCm??undefined,thighCm:editing.thighCm??undefined,armCm:editing.armCm??undefined,notes:editing.notes??undefined}:blank()),[editing])
  const set=(key:keyof ProgressEntryInput,value:string)=>setForm(current=>({...current,[key]:key==='entryDate'||key==='notes'?value:value===''?undefined:Number(value)}))
  return <form className="progress-card entry-form" onSubmit={async event=>{event.preventDefault();setBusy(true);setError('');try{editing?await progressApi.updateEntry(clientId,editing.id,form):await progressApi.createEntry(clientId,form);setForm(blank());onCancel();onSaved()}catch(reason){setError(errorMessage(reason))}finally{setBusy(false)}}}>
    <div className="card-head"><div><p className="eyebrow">{editing?'Ispravka unosa':'Novo očitavanje'}</p><h2>Merenje tela</h2></div>{editing&&<button type="button" className="secondary-button" onClick={onCancel}>Otkaži</button>}</div>
    {error&&<div className="form-error">{error}</div>}<div className="measurement-grid"><label>Datum<input required type="date" value={form.entryDate} onChange={event=>set('entryDate',event.target.value)}/></label>{measurements.map(([key,label,unit])=><label key={key}>{label} ({unit})<input min="0" step="0.1" type="number" value={form[key]??''} onChange={event=>set(key,event.target.value)}/></label>)}</div>
    <label className="notes-label">Beleška<textarea value={form.notes??''} onChange={event=>set('notes',event.target.value)} placeholder="Kratak kontekst merenja…"/></label><button className="primary-button" disabled={busy}>{busy?'Čuvam…':editing?'Sačuvaj izmene':'Sačuvaj merenje'}</button>
  </form>
}

function RecordForm({clientId,records,onSaved,editing,onCancel}:{clientId:number;records:PersonalRecord[];onSaved:()=>void;editing:PersonalRecord|null;onCancel:()=>void}) {
  const blank=():PersonalRecordInput=>({exerciseName:'',value:0,unit:'KG',recordDate:today()})
  const[form,setForm]=useState<PersonalRecordInput>(blank())
  const[busy,setBusy]=useState(false)
  useEffect(()=>setForm(editing?{exerciseName:editing.exerciseName,value:editing.value,unit:editing.unit,recordDate:editing.recordDate}:blank()),[editing])
  const exercises=[...new Set(records.map(record=>record.exerciseName))].sort()
  return <form className="record-inline" onSubmit={async event=>{event.preventDefault();setBusy(true);try{editing?await progressApi.updateRecord(clientId,editing.id,form):await progressApi.createRecord(clientId,form);setForm(blank());onCancel();onSaved()}finally{setBusy(false)}}}>
    <input required list="existing-exercises" placeholder="Vežba" value={form.exerciseName} onChange={event=>setForm({...form,exerciseName:event.target.value})}/><datalist id="existing-exercises">{exercises.map(exercise=><option key={exercise} value={exercise}/>)}</datalist>
    <input required min="0.01" step="0.01" type="number" placeholder="Vrednost" value={form.value||''} onChange={event=>setForm({...form,value:Number(event.target.value)})}/><select value={form.unit} onChange={event=>setForm({...form,unit:event.target.value as RecordUnit})}>{['KG','LB','REPS','SECONDS','MINUTES','METERS','KM'].map(unit=><option key={unit}>{unit}</option>)}</select><input required type="date" value={form.recordDate} onChange={event=>setForm({...form,recordDate:event.target.value})}/><button disabled={busy}>{editing?'Sačuvaj':'＋ Dodaj rekord'}</button>{editing&&<button type="button" onClick={onCancel}>Otkaži</button>}
  </form>
}

export function ProgressPage() {
  const {requestConfirmation,confirmationDialog}=useConfirm()
  const trainer=useAuthStore(state=>state.session?.activeRole)==='TRAINER'
  const[clients,setClients]=useState<ClientSummary[]>([])
  const[clientId,setClientId]=useState<number|null>(null)
  const[entries,setEntries]=useState<ProgressEntry[]>([])
  const[records,setRecords]=useState<PersonalRecord[]>([])
  const[insight,setInsight]=useState<AiInsight|null>(null)
  const[loading,setLoading]=useState(true)
  const[error,setError]=useState('')
  const[insightError,setInsightError]=useState('')
  const[editingEntry,setEditingEntry]=useState<ProgressEntry|null>(null)
  const[editingRecord,setEditingRecord]=useState<PersonalRecord|null>(null)

  useEffect(()=>{if(!trainer)return;progressApi.trainerClients().then(result=>{setClients(result);setClientId(result[0]?.id??null)}).catch(reason=>setError(errorMessage(reason)))},[trainer])
  const load=async(force=false)=>{
    if(trainer&&!clientId)return
    setLoading(true);setError('');setInsightError('')
    try {
      const[dataEntries,dataRecords]=trainer?await Promise.all([progressApi.trainerEntries(clientId!),progressApi.trainerRecords(clientId!)]):await Promise.all([progressApi.myEntries(),progressApi.myRecords()])
      setEntries(dataEntries);setRecords(dataRecords)
      try { setInsight(trainer?await progressApi.trainerSummary(clientId!,force):await progressApi.mySummary()) }
      catch(reason) { setInsightError(errorMessage(reason)) }
    } catch(reason) { setError(errorMessage(reason)) }
    finally { setLoading(false) }
  }
  useEffect(()=>{setEditingEntry(null);setEditingRecord(null);setInsight(null);void load()},[trainer,clientId])
  const selected=clients.find(client=>client.id===clientId)
  const removeEntry=async(id:number)=>{if(clientId&&await requestConfirmation({title:'Brisanje merenja',message:'Obrisati izabrano merenje?',confirmLabel:'Obriši merenje'}))progressApi.deleteEntry(clientId,id).then(()=>load()).catch(reason=>setError(errorMessage(reason)))}
  const removeRecord=async(id:number)=>{if(clientId&&await requestConfirmation({title:'Brisanje rekorda',message:'Obrisati izabrani rekord?',confirmLabel:'Obriši rekord'}))progressApi.deleteRecord(clientId,id).then(()=>load()).catch(reason=>setError(errorMessage(reason)))}

  return <main className="workspace-page progress-page">{confirmationDialog}<header className="workspace-header"><div><p className="eyebrow">{trainer?'Trenerski portal':'Moj razvoj'}</p><h1>Praćenje napretka</h1><p>{trainer?(selected?`Podaci klijenta ${selected.email}`:'Izaberite klijenta kog ste trenirali.'):'Vaša merenja, lični rekordi i personalizovani rezime.'}</p></div>{trainer&&clients.length>0&&<select className="client-picker" value={clientId??''} onChange={event=>setClientId(Number(event.target.value))}>{clients.map(client=><option key={client.id} value={client.id}>{client.email}</option>)}</select>}</header>
    {error&&<div className="content-error">{error}</div>}{trainer&&clients.length===0?<section className="progress-card empty-clients"><span>◎</span><h2>Nema povezanih klijenata</h2><p>Klijenti će se pojaviti nakon što budete trener na njihovom terminu.</p></section>:<>
      <section className="progress-grid"><article className="progress-card chart-card"><div className="card-head"><div><p className="eyebrow">Trend kroz vreme</p><h2>Merenja</h2></div><strong>{entries.length} unosa</strong></div><ProgressChart entries={entries}/><div className="progress-entry-list">{entries.map(entry=><div key={entry.id}><span>{new Date(entry.entryDate).toLocaleDateString('sr-Latn-RS')} · {measurements.map(([key,label,unit])=>`${label}: ${entry[key]??'—'} ${unit}`).join(' · ')}</span>{trainer&&<div><button onClick={()=>setEditingEntry(entry)}>Izmeni</button><button onClick={()=>void removeEntry(entry.id)}>Obriši</button></div>}</div>)}</div></article>{trainer&&clientId&&<EntryForm clientId={clientId} editing={editingEntry} onCancel={()=>setEditingEntry(null)} onSaved={()=>void load()}/>}</section>
      <section className="progress-card records-card"><div className="card-head"><div><p className="eyebrow">Lični maksimumi</p><h2>Rekordi</h2></div><strong>{records.length}</strong></div><PersonalRecordChart records={records}/>{trainer&&clientId&&<RecordForm clientId={clientId} records={records} editing={editingRecord} onCancel={()=>setEditingRecord(null)} onSaved={()=>void load()}/>}<div className="record-list">{records.length?records.map(record=><article key={record.id}><div><span>{record.exerciseName}</span><small>{new Date(record.recordDate).toLocaleDateString('sr-Latn-RS')}</small></div><strong>{record.value} <small>{record.unit}</small></strong>{trainer&&<div className="row-actions"><button onClick={()=>setEditingRecord(record)}>Izmeni</button><button onClick={()=>void removeRecord(record.id)}>Obriši</button></div>}</article>):<div className="empty-panel">Još nema ličnih rekorda.</div>}</div></section>
      <Narrative insight={insight} loading={loading} error={insightError} trainer={trainer} onRefresh={()=>void load(true)}/>
    </>}</main>
}
