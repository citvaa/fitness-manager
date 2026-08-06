import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../api/client'
import { gymApi } from '../api/gym'
import { useOccupancy } from '../hooks/useOccupancy'
import type { Gym, Room, RoomOccupancy } from '../types'

const WIDTH=1000,HEIGHT=620
function status(occupancy:RoomOccupancy|undefined){const ratio=(occupancy?.totalOccupancy??0)/(occupancy?.capacity||1);if(ratio>=1)return{label:'Puno',tone:'full'};if(ratio>=.7)return{label:'Visoka',tone:'busy'};if(ratio>0)return{label:'Aktivna',tone:'active'};return{label:'Slobodna',tone:'quiet'}}

export function LivePlanPage(){
 const wrap=useRef<HTMLDivElement>(null);const [scale,setScale]=useState(1);const [gym,setGym]=useState<Gym|null>(null);const [rooms,setRooms]=useState<Room[]>([]);const [loadError,setLoadError]=useState('');const {snapshot,connected,error}=useOccupancy()
 useEffect(()=>{const ro=new ResizeObserver(([e])=>setScale(Math.min(1,e.contentRect.width/WIDTH)));if(wrap.current)ro.observe(wrap.current);return()=>ro.disconnect()},[])
 useEffect(()=>{Promise.all([gymApi.gym(),gymApi.rooms()]).then(([g,r])=>{setGym(g);setRooms(r)}).catch(e=>setLoadError(errorMessage(e)))},[])
 const byRoom=useMemo(()=>new Map(snapshot?.rooms.map(r=>[r.roomId,r])??[]),[snapshot]);const total=snapshot?.rooms.reduce((sum,r)=>sum+r.totalOccupancy,0)??0;const capacity=snapshot?.rooms.reduce((sum,r)=>sum+r.capacity,0)??rooms.reduce((sum,r)=>sum+r.capacity,0);const active=snapshot?.rooms.filter(r=>r.totalOccupancy>0).length??0
 return <main className="live-page">
  <header className="live-header"><div className="live-title"><div className="live-kicker"><span className={connected?'connected':''}/>{connected?'Uživo povezano':'Povezivanje…'}</div><h1>{gym?.name??'Plan teretane'}</h1><p>{gym?.address??'Pregled prostora i zauzetosti u realnom vremenu'}</p></div><div className="live-time"><small>Poslednje osvežavanje</small><strong>{snapshot?new Date(snapshot.generatedAt).toLocaleTimeString('sr-RS',{hour:'2-digit',minute:'2-digit',second:'2-digit'}):'—'}</strong></div></header>
  {(loadError||error)&&<div className="live-warning">{loadError||error}</div>}
  <section className="metric-row"><article><div className="metric-icon lime">◉</div><div><small>Ljudi u teretani</small><strong className="metric-number">{total}</strong><span>od {capacity} ukupno</span></div></article><article><div className="metric-icon violet">⌂</div><div><small>Aktivne sale</small><strong className="metric-number">{active}</strong><span>od {rooms.length} sala</span></div></article><article className="energy-card"><div><small>Energija prostora</small><strong>{capacity?Math.round(total/capacity*100):0}%</strong></div><div className="energy-bar"><i style={{width:`${Math.min(100,capacity?total/capacity*100:0)}%`}}/></div><span>{total===0?'Mirno — idealno vreme za trening':total/capacity>.7?'Najživlji deo dana':'Prijatan ritam u prostoru'}</span></article></section>
  <section className="live-board"><div className="board-head"><div><p className="eyebrow">Pogled iz ptičije perspektive</p><h2>Live floor</h2></div><div className="legend"><span><i className="quiet"/>Slobodna</span><span><i className="active"/>Aktivna</span><span><i className="busy"/>Visoka</span><span><i className="full"/>Puna</span></div></div>
   <div className="live-canvas-shell" ref={wrap} style={{height:HEIGHT*scale}}><div className="live-canvas" style={{width:WIDTH,height:HEIGHT,transform:`scale(${scale})`}}>{rooms.length===0?<div className="empty-live"><span>⌗</span><h3>Plan još nije nacrtan</h3><p>Dodajte sale kroz editor da biste pokrenuli live prikaz.</p><Link to="/app/editor">Otvori editor →</Link></div>:rooms.map(room=>{const occ=byRoom.get(room.id);const state=status(occ);const count=occ?.totalOccupancy??0;const ratio=Math.min(1,count/room.capacity);return <article key={room.id} className={`live-room ${state.tone}`} style={{left:room.posX,top:room.posY,width:room.width,height:room.height,transform:`rotate(${room.rotationDegrees}deg)`, '--fill':`${ratio*100}%`} as React.CSSProperties}><div className="room-glow"/><div className="room-live-top"><span>{room.type.replace('_',' ')}</span><i>{state.label}</i></div><div className="room-live-center"><strong key={count}>{count}</strong><span>/ {room.capacity}</span><small>osoba trenutno</small></div><div className="room-live-bottom"><b>{room.name}</b><div>{occ?.manualCheckIns??0} check-in · {occ?.scheduledParticipants??0} zakazano</div></div></article>})}</div></div>
  </section>
 </main>
}
