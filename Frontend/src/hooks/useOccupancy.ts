import { useEffect, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { API_URL, errorMessage } from '../api/client'
import { gymApi } from '../api/gym'
import type { OccupancySnapshot } from '../types'

export function useOccupancy() {
  const [snapshot,setSnapshot]=useState<OccupancySnapshot|null>(null)
  const [connected,setConnected]=useState(false)
  const [error,setError]=useState('')
  useEffect(()=>{
    let active=true
    gymApi.occupancy().then(data=>{if(active)setSnapshot(data)}).catch(err=>{if(active)setError(errorMessage(err))})
    const brokerURL=API_URL.replace(/^http/,'ws')+'/ws'
    const client=new Client({brokerURL,reconnectDelay:3000,heartbeatIncoming:10000,heartbeatOutgoing:10000,
      onConnect:()=>{if(!active)return;setConnected(true);setError('');client.subscribe('/topic/gym/occupancy',message=>{try{setSnapshot(JSON.parse(message.body) as OccupancySnapshot)}catch{setError('Primljen je neispravan live snapshot.')}})},
      onWebSocketClose:()=>active&&setConnected(false),onStompError:(frame)=>active&&setError(frame.headers.message??'Live veza je prekinuta.')})
    client.activate()
    return()=>{active=false;void client.deactivate()}
  },[])
  return {snapshot,connected,error}
}
