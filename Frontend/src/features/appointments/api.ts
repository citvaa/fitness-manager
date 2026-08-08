import { http } from '../../lib/http'
import type { AppointmentDTO } from './types'

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
