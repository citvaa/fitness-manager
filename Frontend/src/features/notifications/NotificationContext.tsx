import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useAuthStore } from '../../auth/store'
import { getMyClientId, getMyTrainerId } from './api'
import { useNotificationSocket } from './useNotificationSocket'
import type { PushNotification } from './types'

interface NotificationContextValue {
  notifications: PushNotification[]
  unreadCount: number
  markAllRead: () => void
}

const NotificationContext = createContext<NotificationContextValue | null>(null)

/**
 * Owns the WebSocket subscription(s) for the logged-in user's push notifications - topics are
 * derived from held roles (TRAINER -> /topic/trainer{trainerId}, CLIENT -> /topic/client{clientId}),
 * not the currently *active* role, so a multi-role account (e.g. a trainer who is also a client)
 * keeps receiving both while only one role's nav is visible. Mount once in AppShell so the
 * subscription survives navigation between pages. See AGENTS.md "Upgrade: notification decisions".
 */
export function NotificationProvider({ children }: { children: ReactNode }) {
  const user = useAuthStore((s) => s.user)
  const [topics, setTopics] = useState<string[]>([])
  const [lastReadAt, setLastReadAt] = useState(0)

  useEffect(() => {
    if (!user) {
      setTopics([])
      return
    }

    let cancelled = false
    const lookups: Promise<string | null>[] = []

    if (user.roles.includes('TRAINER')) {
      lookups.push(
        getMyTrainerId()
          .then((id) => `/topic/trainer${id}`)
          .catch(() => null),
      )
    }
    if (user.roles.includes('CLIENT')) {
      lookups.push(
        getMyClientId()
          .then((id) => `/topic/client${id}`)
          .catch(() => null),
      )
    }
    // /topic/manager is a fixed broadcast topic (every manager gets the same alerts, no
    // per-manager id) - see ManagerAlertNotificationDTO for why this is push-only.
    if (user.roles.includes('MANAGER')) {
      lookups.push(Promise.resolve('/topic/manager'))
    }

    Promise.all(lookups).then((resolved) => {
      if (!cancelled) setTopics(resolved.filter((t): t is string => t !== null))
    })

    return () => {
      cancelled = true
    }
  }, [user])

  const { notifications } = useNotificationSocket(topics)
  const unreadCount = notifications.filter((n) => n.receivedAt > lastReadAt).length

  const value = useMemo<NotificationContextValue>(
    () => ({ notifications, unreadCount, markAllRead: () => setLastReadAt(Date.now()) }),
    [notifications, unreadCount],
  )

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>
}

export function useNotifications() {
  const ctx = useContext(NotificationContext)
  if (!ctx) throw new Error('useNotifications must be used within a NotificationProvider')
  return ctx
}
