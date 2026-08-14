export type NotificationPreference = 'EMAIL' | 'PUSH' | 'BOTH'

export interface PushNotification {
  id: string
  message: string
  receivedAt: number
}
