import { useEffect, useState } from 'react'
import { errorMessage } from '../api/client'
import { insightsApi } from '../api/insights'
import type { InsightRating, ManagerInsight } from '../types'

const ratingLabel:Record<InsightRating,string>={EXCELLENT:'Odlično',GOOD:'Dobro',AVERAGE:'Prosečno',POOR:'Slabo'}

export function ManagerInsightsPage(){
 const[insight,setInsight]=useState<ManagerInsight|null>(null);const[loading,setLoading]=useState(true);const[error,setError]=useState('')
 const load=async(force=false)=>{setLoading(true);setError('');try{setInsight(await insightsApi.manager(force))}catch(reason){setError(errorMessage(reason))}finally{setLoading(false)}}
 useEffect(()=>{void load()},[])
 return <main className="workspace-page insights-page">
  <header className="workspace-header"><div><p className="eyebrow">Claude analiza · poslednjih 30 dana</p><h1>AI uvidi</h1><p>Izračunati operativni pokazatelji sa kratkom AI ocenom i predlogom.</p></div><button className="primary-button compact" disabled={loading} onClick={()=>void load(true)}>{loading?'Analiziram…':'↻ Regeneriši'}</button></header>
  {error&&<div className="content-error">{error}<button onClick={()=>void load()}>Pokušaj ponovo</button></div>}
  {loading&&!insight?<section className="insight-hero"><div className="insight-skeleton"><i/><i/><i/></div></section>:insight&&<>
   <section className="insight-summary"><div><p className="eyebrow">Rezime</p><h2>{insight.summary}</h2><small>Generisano {new Date(insight.generatedAt).toLocaleString('sr-RS')} · {insight.model}</small></div><ol>{insight.recommendations.map((recommendation,index)=><li key={index}>{recommendation}</li>)}</ol></section>
   <section className="insight-metric-grid">{insight.metrics.map(metric=><article className="insight-metric" key={metric.key}><div className="card-head"><h3>{metric.label}</h3><span className={`rating-badge ${metric.rating.toLowerCase()}`}>{ratingLabel[metric.rating]}</span></div><strong>{metric.value.toLocaleString('sr-RS')} <small>{metric.unit}</small></strong><div className="metric-visual"><i style={{width:`${Math.min(100,Math.max(0,metric.unit==='%'?metric.value:metric.value))}%`}}/></div><p>{metric.comment}</p></article>)}</section>
  </>}
  <p className="ai-disclaimer">Brojevi su izračunati iz baze; AI daje samo ocenu i preporuku. Proverite važne zaključke u izvornim podacima.</p>
 </main>
}
