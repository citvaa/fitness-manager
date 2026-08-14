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

export interface RoomSummaryDTO {
  id: number
  name: string
}

export interface AppointmentDTO {
  id: number
  date: string // ISO LocalDate
  startTime: string // "HH:mm:ss"
  endTime: string // "HH:mm:ss"
  session: SessionDTO
  trainer: TrainerSummaryDTO | null
  // Present on the wire (backend's shared AppointmentDTO always includes it) but missing from
  // this feature's local type until the trainer-testing round - see AGENTS.md "Upgrade: trainer
  // self-assign decisions".
  room: RoomSummaryDTO | null
  clients: ClientSummaryDTO[]
}

/** Backs the TRAINER "Započni trening" client check-in panel - see AGENTS.md "Upgrade: trainer
 * check-in decisions". */
export interface RoomCheckInDTO {
  id: number
  room: RoomSummaryDTO
  client: ClientSummaryDTO
  checkedInAt: string // ISO LocalDateTime
  checkedOutAt: string | null
}
