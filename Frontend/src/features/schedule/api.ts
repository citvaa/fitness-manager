import { http } from '../../lib/http'
import type {
  CreateOwnTrainerScheduleRequest,
  CreateOwnTrainerUnavailabilityRequest,
  MyAppointmentSlimDTO,
  TrainerScheduleDTO,
} from './types'

/** TRAINER self-service - see AGENTS.md "Upgrade: Faza 6 decisions" for the backend endpoints added for this. */

export function getMySchedule() {
  return http.get<TrainerScheduleDTO[]>('/api/schedule/trainer/me').then((r) => r.data)
}

export function createMySchedule(request: CreateOwnTrainerScheduleRequest) {
  return http.post<TrainerScheduleDTO>('/api/schedule/trainer/me', request).then((r) => r.data)
}

/** Backs the "fiksni raspored" (weekly-recurring) option - see AGENTS.md "Upgrade: trainer
 * fixed-schedule decisions" for how many weekly instances the backend generates. */
export function createMyScheduleRecurring(request: CreateOwnTrainerScheduleRequest) {
  return http.post<TrainerScheduleDTO[]>('/api/schedule/trainer/me/recurring', request).then((r) => r.data)
}

/** Reuses the TRAINER's own "assigned to me" appointment endpoint purely to cross-reference
 * against WORKING schedule entries (B2) - narrowed to MyAppointmentSlimDTO's shape client-side. */
export function getMyAppointmentsForScheduleCheck() {
  return http.get<MyAppointmentSlimDTO[]>('/api/appointment/trainer/me').then((r) => r.data)
}

export function createMyUnavailability(request: CreateOwnTrainerUnavailabilityRequest) {
  return http.post('/api/schedule/trainer/me/unavailable', request)
}

export function deleteMyScheduleEntry(id: number) {
  return http.delete(`/api/schedule/trainer/${id}`)
}
