import { http } from '../../lib/http'
import type { Role } from '../../auth/types'
import type {
  ClientDTO,
  CreateGymScheduleRequest,
  CreateHolidayRequest,
  CreateTrainerRequest,
  CreateTrainerScheduleRequest,
  CreateTrainerUnavailabilityRequest,
  GymScheduleDTO,
  HolidayDTO,
  PageResponse,
  TrainerDTO,
  TrainerScheduleDTO,
  UserDTO,
} from './types'

// ---- Users (generic accounts) ----

export function getUsers(page: number, size: number, search?: string) {
  return http
    .get<PageResponse<UserDTO>>('/api/user', { params: { page, size, search: search || undefined } })
    .then((r) => r.data)
}

export function createUser(email: string) {
  return http.post<UserDTO>('/api/user', { email }).then((r) => r.data)
}

export function updateUser(id: number, email: string) {
  return http.put<UserDTO>(`/api/user/${id}`, { email }).then((r) => r.data)
}

export function deleteUser(id: number) {
  return http.delete(`/api/user/${id}`)
}

export function addUserRole(id: number, role: Role) {
  return http.post(`/api/user/${id}/role`, null, { params: { role } })
}

export function removeUserRole(id: number, role: Role) {
  return http.delete(`/api/user/${id}/role`, { params: { role } })
}

// ---- Trainers (domain profile) ----

export function getTrainers() {
  return http.get<TrainerDTO[]>('/api/trainer').then((r) => r.data)
}

export function createTrainer(request: CreateTrainerRequest) {
  return http.post<TrainerDTO>('/api/trainer', request).then((r) => r.data)
}

export function updateTrainer(id: number, request: CreateTrainerRequest) {
  return http.put<TrainerDTO>(`/api/trainer/${id}`, request).then((r) => r.data)
}

export function deleteTrainer(id: number) {
  return http.delete(`/api/trainer/${id}`)
}

// ---- Clients (domain profile) ----

export function getClients() {
  return http.get<ClientDTO[]>('/api/client').then((r) => r.data)
}

export function createClient(email: string) {
  return http.post<ClientDTO>('/api/client', { email }).then((r) => r.data)
}

// ---- Gym opening hours ----

export function getGymSchedule() {
  return http.get<GymScheduleDTO[]>('/api/schedule/gym').then((r) => r.data)
}

export function upsertGymScheduleDay(request: CreateGymScheduleRequest) {
  return http.post<GymScheduleDTO>('/api/schedule/gym', request).then((r) => r.data)
}

// ---- Holidays ----

export function getHolidays() {
  return http.get<HolidayDTO[]>('/api/schedule/holiday').then((r) => r.data)
}

export function createHoliday(request: CreateHolidayRequest) {
  return http.post<HolidayDTO>('/api/schedule/holiday', request).then((r) => r.data)
}

// ---- Trainer schedule (manager oversight) ----

export function getTrainerSchedule(trainerId: number) {
  return http.get<TrainerScheduleDTO[]>(`/api/schedule/trainer/${trainerId}`).then((r) => r.data)
}

export function createTrainerSchedule(request: CreateTrainerScheduleRequest) {
  return http.post<TrainerScheduleDTO>('/api/schedule/trainer', request).then((r) => r.data)
}

export function createTrainerUnavailability(request: CreateTrainerUnavailabilityRequest) {
  return http.post('/api/schedule/trainer/unavailable', request)
}

export function deleteTrainerScheduleEntry(id: number) {
  return http.delete(`/api/schedule/trainer/${id}`)
}
