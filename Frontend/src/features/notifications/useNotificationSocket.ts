import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { API_BASE_URL } from '../../lib/http'
import type { PushNotification } from './types'

const MAX_KEPT = 30

/**
 * Subscribes to one or more STOMP topics (e.g. /topic/trainer{id}, /topic/client{id}) and
 * accumulates their {message: string} payloads into a bounded, newest-first list. Modeled on
 * gym/useOccupancySocket.ts's connection handling, but generic over topics since a single user
 * can hold both TRAINER and CLIENT roles and needs both topics live at once regardless of which
 * role is currently active (see NotificationProvider).
 */
export function useNotificationSocket(topics: string[]) {
  const [notifications, setNotifications] = useState<PushNotification[]>([])
  const counterRef = useRef(0)
  const topicsKey = topics.join('|')

  useEffect(() => {
    if (topics.length === 0) return

    const wsUrl = API_BASE_URL.replace(/^http/, 'ws') + '/ws'
    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        topics.forEach((topic) => {
          client.subscribe(topic, (frame) => {
            try {
              const payload = JSON.parse(frame.body) as { message: string }
              counterRef.current += 1
              const notification: PushNotification = {
                id: `${topic}-${counterRef.current}`,
                message: payload.message,
                receivedAt: Date.now(),
              }
              setNotifications((prev) => [notification, ...prev].slice(0, MAX_KEPT))
            } catch {
              // ignore malformed frame
            }
          })
        })
      },
    })

    client.activate()
    return () => {
      void client.deactivate()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [topicsKey])

  return { notifications }
}
