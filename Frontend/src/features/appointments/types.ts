export type SessionType = 'INDIVIDUAL' | 'GROUP'

export interface SessionDTO {
  id: number
  type: SessionType
  maxParticipants: number
}

export interface TrainerSummaryDTO {
  id: number
  email: string
}

export interface ClientSummaryDTO {
  id: number
  email: string
}

export interface AppointmentDTO {
  id: number
  date: string // ISO LocalDate
  startTime: string // "HH:mm:ss"
  endTime: string // "HH:mm:ss"
  session: SessionDTO
  trainer: TrainerSummaryDTO | null
  clients: ClientSummaryDTO[]
}
