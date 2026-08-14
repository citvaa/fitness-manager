import { http } from '../../lib/http'
import type { NotificationPreference } from './types'

/** GET /api/user/me - any authenticated role, resolves the caller from the JWT. */
export function getMyNotificationPreference() {
  return http
    .get<{ notificationPreference: NotificationPreference }>('/api/user/me')
    .then((r) => r.data.notificationPreference)
}

export function updateMyNotificationPreference(notificationPreference: NotificationPreference) {
  return http.patch('/api/user/me/notification-preference', null, { params: { notificationPreference } })
}

/** Resolves the caller's own trainer/client id - needed to subscribe to their
 * /topic/trainer{id} or /topic/client{id} push-notification topic (the id is
 * a Trainer/Client id, not the User id from the JWT). */
export function getMyTrainerId() {
  return http.get<{ id: number }>('/api/trainer/me').then((r) => r.data.id)
}

export function getMyClientId() {
  return http.get<{ id: number }>('/api/client/me').then((r) => r.data.id)
}
