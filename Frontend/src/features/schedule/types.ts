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

/** Just enough of the backend's shared AppointmentDTO to cross-reference a trainer's own
 * assigned appointments against their WORKING schedule entries (B2 - see AGENTS.md "Upgrade:
 * trainer fixed-schedule decisions"). Deliberately duplicated/narrowed here rather than importing
 * features/appointments/types.ts's full AppointmentDTO - same "small duplication over cross-
 * feature coupling" convention as features/admin/api.ts's RoomDTO duplication. */
export interface MyAppointmentSlimDTO {
  id: number
  date: string
  startTime: string
  endTime: string
}
