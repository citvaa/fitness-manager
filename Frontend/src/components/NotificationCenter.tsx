import { useEffect, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { API_URL, api } from '../api/client'
import { useAuthStore } from '../auth/authStore'
import { decodeJwt } from '../auth/token'
import type { ClientProfile, TrainerProfile, UserAccount } from '../types'

export function NotificationCenter(){
  const session=useAuthStore(s=>s.session)!;const roles=decodeJwt(session.accessToken).roles??[]
  const[messages,setMessages]=useState<string[]>([]);const[user,setUser]=useState<UserAccount|null>(null)
  useEffect(()=>{void api.get<UserAccount>('/api/user/me').then(r=>setUser(r.data));const ws=new Client({brokerURL:API_URL.replace(/^http/,'ws')+'/ws',reconnectDelay:3000,onConnect:async()=>{const topics:string[]=[];if(roles.includes('TRAINER'))topics.push(`/topic/trainer${(await api.get<TrainerProfile>('/api/trainer/me')).data.id}`);if(roles.includes('CLIENT'))topics.push(`/topic/client${(await api.get<ClientProfile>('/api/client/me')).data.id}`);topics.forEach(topic=>ws.subscribe(topic,message=>{let text=message.body;try{const payload=JSON.parse(message.body) as {message?:unknown};if(typeof payload.message==='string')text=payload.message}catch{/* retain plain payload */}setMessages(old=>[text,...old].slice(0,30))}))}});ws.activate();return()=>{void ws.deactivate()}},[session.accessToken])
  const change=async(value:UserAccount['notificationPreference'])=>{await api.patch('/api/user/me/notification-preference',null,{params:{notificationPreference:value}});setUser(current=>current?{...current,notificationPreference:value}:current)}
  return <div className="notification-center"><details><summary><span aria-hidden="true">🔔</span><span>Centar obaveštenja</span>{messages.length>0&&<strong>{messages.length}</strong>}</summary><div className="notification-panel">{messages.length?messages.map((message,index)=><p key={index}>{message}</p>):<p>Nema novih obaveštenja.</p>}</div></details><label>Obaveštenja<select className="notification-preference" value={user?.notificationPreference??'PUSH'} onChange={e=>void change(e.target.value as UserAccount['notificationPreference'])}><option value="PUSH">Push</option><option value="EMAIL">Email</option><option value="BOTH">Oba</option></select></label></div>
}
