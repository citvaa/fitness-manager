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

/** Per-SessionType held-vs-paid comparison - see AGENTS.md "Upgrade: payment debt tracking
 * decisions". `owed` is `max(0, held - paid)`, never negative. */
export interface SessionTypePaymentStatusDTO {
  type: SessionType
  held: number
  paid: number
  owed: number
}
