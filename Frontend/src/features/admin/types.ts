import type { Role } from '../../auth/types'

export interface UserDTO {
  id: number
  email: string
  roles: Role[]
  notificationPreference: 'EMAIL' | 'PUSH' | 'BOTH'
  isActivated: boolean
  registrationKey: string | null
  registrationKeyValidity: string | null
  resetKey: string | null
  resetKeyValidity: string | null
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export type EmploymentStatus = 'FULL_TIME' | 'CONTRACT' | 'FORMER_EMPLOYEE'

export interface TrainerDTO {
  id: number
  user: UserDTO
  employmentDate: string
  birthYear: number
  status: EmploymentStatus
}

export interface ClientDTO {
  id: number
  user: UserDTO
}

export interface CreateUserRequest {
  email: string
}

export interface CreateTrainerRequest {
  email: string
  employmentDate: string
  birthYear: number
  status: EmploymentStatus
}

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

export interface GymScheduleDTO {
  id: number
  day: DayOfWeek
  openingTime: string
  closingTime: string
}

export interface CreateGymScheduleRequest {
  day: DayOfWeek
  startTime: string
  endTime: string
}

export interface HolidayDTO {
  id: number
  date: string
  description: string
}

export interface CreateHolidayRequest {
  date: string
  description: string
}

export type WorkStatus = 'WORKING' | 'HOLIDAY' | 'SICK_LEAVE' | 'VACATION'

export interface TrainerScheduleDTO {
  id: number
  trainer: TrainerDTO
  date: string
  startTime: string
  endTime: string
  status: WorkStatus
}

// ---- Appointment slot management (Faza 9) ----
// Minimal duplicate of features/appointments/types.ts's shape, extended with `room` (wired into
// AppointmentDTO/CreateAppointmentRequest this phase - see AGENTS.md "Upgrade: Faza 9
// decisions"). Kept local to this feature rather than importing across features, matching this
// codebase's existing tolerance for small duplication over cross-feature coupling.

export type SessionType = 'INDIVIDUAL' | 'GROUP'

export interface SessionDTO {
  id: number
  type: SessionType
  maxParticipants: number
}

export interface AppointmentTrainerSummary {
  id: number
  email: string
}

export interface AppointmentClientSummary {
  id: number
  email: string
}

export interface RoomOptionDTO {
  id: number
  name: string
  /** Backs the "Tip sesije" picker filter (a session type only fits a room whose capacity is at
   * least that type's maxParticipants) - see AGENTS.md "Upgrade: appointment picker filtering
   * decisions". */
  capacity: number
}

export interface AppointmentDTO {
  id: number
  date: string
  startTime: string
  endTime: string
  session: SessionDTO
  trainer: AppointmentTrainerSummary | null
  room: RoomOptionDTO | null
  clients: AppointmentClientSummary[]
}

export interface CreateAppointmentRequest {
  date: string
  startTime: string
  endTime: string
  sessionId: number
  // Room stays mandatory (manager-testing round 3 - an appointment with no room made occupancy
  // tracking meaningless). Trainer is optional again (trainer self-assign round) - a manager can
  // create an open "termin bez trenera" slot for a TRAINER to self-assign to later. See AGENTS.md
  // "Upgrade: trainer self-assign decisions".
  trainerId: number | null
  roomId: number | null
  clientIds?: number[]
  /** When true, `date` is the first occurrence of a weekly-recurring slot - backend generates
   * further weekly instances (see AGENTS.md). Only meaningful for createRecurringAppointment(). */
  recurring?: boolean
}
