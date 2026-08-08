export type WorkStatus = 'WORKING' | 'HOLIDAY' | 'SICK_LEAVE' | 'VACATION'

export interface TrainerScheduleDTO {
  id: number
  date: string
  startTime: string
  endTime: string
  status: WorkStatus
}

export interface CreateOwnTrainerScheduleRequest {
  date: string
  startTime: string
  endTime: string
}

export interface CreateOwnTrainerUnavailabilityRequest {
  startDate: string
  endDate: string
  status: WorkStatus
}
