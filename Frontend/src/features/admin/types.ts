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

export interface CreateTrainerScheduleRequest {
  trainerId: number
  date: string
  startTime: string
  endTime: string
}

export interface CreateTrainerUnavailabilityRequest {
  trainerId: number
  startDate: string
  endDate: string
  status: WorkStatus
}
