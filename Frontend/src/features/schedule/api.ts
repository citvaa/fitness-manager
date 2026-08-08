import { http } from '../../lib/http'
import type {
  CreateOwnTrainerScheduleRequest,
  CreateOwnTrainerUnavailabilityRequest,
  TrainerScheduleDTO,
} from './types'

/** TRAINER self-service - see AGENTS.md "Upgrade: Faza 6 decisions" for the backend endpoints added for this. */

export function getMySchedule() {
  return http.get<TrainerScheduleDTO[]>('/api/schedule/trainer/me').then((r) => r.data)
}

export function createMySchedule(request: CreateOwnTrainerScheduleRequest) {
  return http.post<TrainerScheduleDTO>('/api/schedule/trainer/me', request).then((r) => r.data)
}

export function createMyUnavailability(request: CreateOwnTrainerUnavailabilityRequest) {
  return http.post('/api/schedule/trainer/me/unavailable', request)
}

export function deleteMyScheduleEntry(id: number) {
  return http.delete(`/api/schedule/trainer/${id}`)
}
