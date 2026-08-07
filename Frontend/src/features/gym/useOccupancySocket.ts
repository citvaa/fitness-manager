import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { API_BASE_URL } from '../../lib/http'
import { getOccupancy } from './api'
import type { RoomOccupancyDTO } from './types'

/**
 * Loads the initial occupancy snapshot over HTTP, then subscribes to
 * /topic/gym/occupancy for live updates (see AGENTS.md "Upgrade: service
 * layer decisions" - one topic, full room-list snapshot every message, no
 * per-room diffing needed here).
 */
export function useOccupancySocket() {
  const [occupancy, setOccupancy] = useState<RoomOccupancyDTO[]>([])
  const [connected, setConnected] = useState(false)
  // Distinguishes "still connecting for the first time" from "was live, then
  // dropped" - only the latter should read as a disconnect to the user (see
  // AGENTS.md "Upgrade: frontend decisions" WebSocket section).
  const [everConnected, setEverConnected] = useState(false)
  const clientRef = useRef<Client | null>(null)

  useEffect(() => {
    let cancelled = false
    getOccupancy()
      .then((data) => {
        if (!cancelled) setOccupancy(data)
      })
      .catch(() => {
        /* live view still works once the socket delivers the first update */
      })

    const wsUrl = API_BASE_URL.replace(/^http/, 'ws') + '/ws'
    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true)
        setEverConnected(true)
        client.subscribe('/topic/gym/occupancy', (message) => {
          try {
            const data = JSON.parse(message.body) as RoomOccupancyDTO[]
            setOccupancy(data)
          } catch {
            // ignore malformed frame
          }
        })
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
    })

    clientRef.current = client
    client.activate()

    return () => {
      cancelled = true
      void client.deactivate()
    }
  }, [])

  return { occupancy, connected, everConnected }
}
