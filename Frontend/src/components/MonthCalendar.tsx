import { useEffect, useMemo, useState } from 'react'

const iso=(date:Date)=>`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
export function MonthCalendar({value,onChange,highlightedDates}:{value:string;onChange:(date:string)=>void;highlightedDates:Set<string>}) {
  const selected=new Date(`${value}T12:00:00`)
  const[month,setMonth]=useState(()=>new Date(selected.getFullYear(),selected.getMonth(),1))
  useEffect(()=>setMonth(new Date(selected.getFullYear(),selected.getMonth(),1)),[value])
  const days=useMemo(()=>{const result:(Date|null)[]=Array((month.getDay()+6)%7).fill(null);for(let day=1;day<=new Date(month.getFullYear(),month.getMonth()+1,0).getDate();day++)result.push(new Date(month.getFullYear(),month.getMonth(),day));return result},[month])
  return <section className="month-calendar"><header><button type="button" aria-label="Prethodni mesec" onClick={()=>setMonth(new Date(month.getFullYear(),month.getMonth()-1,1))}>‹</button><strong>{month.toLocaleDateString('sr-Latn-RS',{month:'long',year:'numeric'})}</strong><button type="button" aria-label="Sledeći mesec" onClick={()=>setMonth(new Date(month.getFullYear(),month.getMonth()+1,1))}>›</button></header><div className="month-weekdays">{['Po','Ut','Sr','Če','Pe','Su','Ne'].map(day=><span key={day}>{day}</span>)}</div><div className="month-grid">{days.map((date,index)=>date?<button type="button" key={iso(date)} className={`${iso(date)===value?'selected ':''}${highlightedDates.has(iso(date))?'highlighted':''}`} onClick={()=>onChange(iso(date))}>{date.getDate()}<i/></button>:<span key={`blank-${index}`}/>)}</div></section>
}
