export interface ClientSummaryDTO {
  id: number
  email: string
}

export interface TrainerSummaryDTO {
  id: number
  email: string
}

export interface RoomSummaryDTO {
  id: number
  name: string
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
  // Present on the wire (backend's shared AppointmentDTO always includes it) but missing from
  // this feature's local type until the manager-testing round 3 calendar restructure - see
  // AGENTS.md.
  room: RoomSummaryDTO | null
  clients: ClientSummaryDTO[] | null
}

export interface DailyScheduleDTO {
  date: string
  appointments: AppointmentDTO[]
}
