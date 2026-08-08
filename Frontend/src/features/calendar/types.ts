export interface ClientSummaryDTO {
  id: number
  email: string
}

export interface TrainerSummaryDTO {
  id: number
  email: string
}

export interface SessionDTO {
  id: number
  type: 'INDIVIDUAL' | 'GROUP'
  maxParticipants: number
}

export interface AppointmentDTO {
  id: number
  date: string
  startTime: string
  endTime: string
  session: SessionDTO | null
  trainer: TrainerSummaryDTO | null
  clients: ClientSummaryDTO[] | null
}

export interface DailyScheduleDTO {
  date: string
  appointments: AppointmentDTO[]
}
