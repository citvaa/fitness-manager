export type SessionType = 'INDIVIDUAL' | 'GROUP'

export interface ClientSummaryDTO {
  id: number
  email: string
}

export interface SessionDTO {
  id: number
  type: SessionType
  maxParticipants: number
}

export interface PaymentDTO {
  id: number
  client: ClientSummaryDTO
  session: SessionDTO
  paidAppointments: number
  paymentDate: string
}

export interface CreatePaymentRequest {
  clientId: number
  sessionId: number
  paidAppointments: number
  paymentDate: string
}
