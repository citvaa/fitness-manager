import { http } from '../../lib/http'
import type { AppointmentDTO, RoomCheckInDTO } from './types'

/**
 * Backend "marketplace" model - see AGENTS.md "Upgrade: Faza 7 decisions": a MANAGER creates
 * slots, a TRAINER self-assigns to an unassigned one, a CLIENT reserves a slot with a free spot.
 * `/available` and `/without-trainer` have no date filter server-side (they return every
 * appointment matching the capacity/trainer condition, past included) - pages here filter to
 * "upcoming" client-side rather than that being fixed on the backend, since fixing it would be
 * a behavior change beyond this phase's scope.
 */

export function getAvailableAppointments() {
  return http.get<AppointmentDTO[]>('/api/appointment/available').then((r) => r.data)
}

export function reserveAppointment(id: number) {
  return http.post<AppointmentDTO>(`/api/appointment/${id}/reserve`).then((r) => r.data)
}

export function cancelAppointment(id: number) {
  return http.delete<AppointmentDTO>(`/api/appointment/${id}/cancel`).then((r) => r.data)
}

export function getMyAppointmentsAsClient() {
  return http.get<AppointmentDTO[]>('/api/appointment/me').then((r) => r.data)
}

export function getAppointmentsWithoutTrainer() {
  return http.get<AppointmentDTO[]>('/api/appointment/without-trainer').then((r) => r.data)
}

export function assignSelfToAppointment(id: number) {
  return http.post<AppointmentDTO>(`/api/appointment/${id}/assign`).then((r) => r.data)
}

export function unassignSelfFromAppointment(id: number) {
  return http.delete<AppointmentDTO>(`/api/appointment/${id}/unassign`).then((r) => r.data)
}

export function getMyAppointmentsAsTrainer() {
  return http.get<AppointmentDTO[]>('/api/appointment/trainer/me').then((r) => r.data)
}

/**
 * Room check-in/check-out - see AGENTS.md "Upgrade: trainer check-in decisions". Independent of
 * the Appointment model (pre-existing endpoints, `/api/gym/...` - not under `/api/appointment`);
 * a live occupancy WebSocket broadcast already fires from these on the backend, so no extra
 * wiring is needed here for the floor-plan view to pick up the change.
 */

/** 204 (no active check-in) maps to `null` rather than propagating an empty-body 200, so callers
 * can branch on the return value alone. */
export function getActiveCheckIn(clientId: number) {
  return http
    .get<RoomCheckInDTO>(`/api/gym/check-in/active/${clientId}`)
    .then((r) => (r.status === 204 ? null : r.data))
}

export function checkInClient(roomId: number, clientId: number) {
  return http
    .post<RoomCheckInDTO>(`/api/gym/room/${roomId}/check-in`, null, { params: { clientId } })
    .then((r) => r.data)
}

export function checkOutClient(checkInId: number) {
  return http.post<RoomCheckInDTO>(`/api/gym/check-in/${checkInId}/check-out`).then((r) => r.data)
}
