import { http } from '../../lib/http'
import type { Role } from '../../auth/types'
import type {
  AppointmentDTO,
  ClientDTO,
  CreateAppointmentRequest,
  CreateGymScheduleRequest,
  CreateHolidayRequest,
  CreateTrainerRequest,
  CreateTrainerScheduleRequest,
  CreateTrainerUnavailabilityRequest,
  GymScheduleDTO,
  HolidayDTO,
  PageResponse,
  RoomOptionDTO,
  SessionDTO,
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

// ---- Appointment slot management (Faza 9) ----
// The "marketplace" endpoints (create/add-trainer/remove-trainer/add-clients/remove-client)
// existed since Faza 2/7 but had no frontend caller at all - see AGENTS.md
// ("Upgrade: Faza 9 decisions"). GET /api/appointment (plain, unfiltered) is new this phase,
// added specifically to back this management list - getAvailable()/getAllWithoutTrainer() each
// filter for a different self-service use case and don't fit here.

export function getAllAppointments() {
  return http.get<AppointmentDTO[]>('/api/appointment').then((r) => r.data)
}

export function createAppointment(request: CreateAppointmentRequest) {
  return http.post<AppointmentDTO>('/api/appointment', request).then((r) => r.data)
}

/** Backs the "fiksni termin" (weekly-recurring) option in the create form - see AGENTS.md
 * "Upgrade: fixed weekly appointment decisions" for how many weekly instances the backend
 * generates from `request.date` onward. */
export function createRecurringAppointment(request: CreateAppointmentRequest) {
  return http.post<AppointmentDTO[]>('/api/appointment/recurring', request).then((r) => r.data)
}

export function assignTrainerToAppointment(appointmentId: number, trainerId: number) {
  return http
    .post<AppointmentDTO>(`/api/appointment/${appointmentId}/add-trainer`, null, { params: { trainerId } })
    .then((r) => r.data)
}

export function removeTrainerFromAppointment(appointmentId: number) {
  return http.delete<AppointmentDTO>(`/api/appointment/${appointmentId}/remove-trainer`).then((r) => r.data)
}

export function addClientToAppointment(appointmentId: number, clientId: number) {
  // Set<Integer> clientIds is bound from repeated query params server-side - built manually
  // rather than via axios's `params` object so the wire format doesn't depend on axios's array
  // serialization defaults (bracket vs. repeat vs. comma-joined all behave differently there).
  return http
    .post<AppointmentDTO>(`/api/appointment/${appointmentId}/add-clients?clientIds=${clientId}`)
    .then((r) => r.data)
}

export function removeClientFromAppointment(appointmentId: number, clientId: number) {
  return http
    .delete<AppointmentDTO>(`/api/appointment/${appointmentId}/remove-client`, { params: { clientId } })
    .then((r) => r.data)
}

export function getSessionsForPicker() {
  return http.get<SessionDTO[]>('/api/session').then((r) => r.data)
}

/** Just enough to populate the "new appointment" room picker - duplicates RoomDTO's shape from
 * features/gym rather than importing across features (see the same reasoning on getClients()
 * duplication in features/payments/api.ts). */
export function getRoomsForPicker() {
  return http
    .get<{ id: number; name: string }[]>('/api/gym/room')
    .then((r): RoomOptionDTO[] => r.data.map((room) => ({ id: room.id, name: room.name })))
}
