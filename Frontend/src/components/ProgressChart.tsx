import { useEffect, useMemo, useState } from 'react'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { PersonalRecord, ProgressEntry } from '../types'
const series=[['weightKg','Težina','#70a929'],['bodyFatPercent','Masti %','#8060ad'],['waistCm','Struk','#e09a36'],['chestCm','Grudi','#3b8b81'],['hipCm','Kukovi','#cf6679'],['thighCm','Butina','#5086c4'],['armCm','Ruka','#97704c']] as const
export function ProgressChart({entries}:{entries:ProgressEntry[]}){if(!entries.length)return <div className="empty-panel">Još nema merenja za prikaz.</div>;return <div className="chart-wrap"><ResponsiveContainer width="100%" height={330}><LineChart data={entries} margin={{top:12,right:18,left:-10,bottom:0}}><CartesianGrid stroke="#e7ece6" strokeDasharray="4 4"/><XAxis dataKey="entryDate" tick={{fontSize:11}}/><YAxis tick={{fontSize:11}} domain={['auto','auto']}/><Tooltip contentStyle={{borderRadius:12,borderColor:'#dce3dc'}}/>{series.map(([key,label,color])=><Line key={key} connectNulls type="monotone" dataKey={key} name={label} stroke={color} strokeWidth={2.5} dot={{r:3}}/>)}</LineChart></ResponsiveContainer><div className="chart-legend">{series.map(([,label,color])=><span key={label}><i style={{background:color}}/>{label}</span>)}</div></div>}

export function PersonalRecordChart({records}:{records:PersonalRecord[]}) {
  const exercises=useMemo(()=>Object.entries(records.reduce<Record<string,number>>((counts,record)=>({...counts,[record.exerciseName]:(counts[record.exerciseName]??0)+1}),{})).sort((a,b)=>b[1]-a[1]).map(([name])=>name),[records])
  const[selected,setSelected]=useState('')
  useEffect(()=>{if(!exercises.includes(selected))setSelected(exercises[0]??'')},[exercises,selected])
  const data=useMemo(()=>records.filter(record=>record.exerciseName===selected).sort((a,b)=>a.recordDate.localeCompare(b.recordDate)),[records,selected])
  if(!records.length)return <div className="empty-panel">Još nema rekorda za graf.</div>
  return <div className="chart-wrap personal-record-chart"><label className="record-chart-filter">Vežba<select value={selected} onChange={event=>setSelected(event.target.value)}>{exercises.map(exercise=><option key={exercise}>{exercise}</option>)}</select></label><ResponsiveContainer width="100%" height={260}><LineChart data={data} margin={{top:12,right:18,left:-10,bottom:0}}><CartesianGrid stroke="#e7ece6" strokeDasharray="4 4"/><XAxis dataKey="recordDate" tick={{fontSize:11}}/><YAxis tick={{fontSize:11}} domain={['auto','auto']}/><Tooltip contentStyle={{borderRadius:12,borderColor:'#dce3dc'}}/><Line type="monotone" dataKey="value" name={selected} stroke="#70a929" strokeWidth={3} dot={{r:4}}/></LineChart></ResponsiveContainer><small>{data[0]?.unit}</small></div>
}
