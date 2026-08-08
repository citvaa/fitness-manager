import { http } from '../../lib/http'
import type { ClientSummaryDTO, CreatePaymentRequest, PaymentDTO, SessionDTO } from './types'

export function getAllPayments(clientId?: number) {
  return http.get<PaymentDTO[]>('/api/payment', { params: { clientId } }).then((r) => r.data)
}

export function getMyPayments() {
  return http.get<PaymentDTO[]>('/api/payment/me').then((r) => r.data)
}

export function createPayment(request: CreatePaymentRequest) {
  return http.post<PaymentDTO>('/api/payment', request).then((r) => r.data)
}

export function getSessions() {
  return http.get<SessionDTO[]>('/api/session').then((r) => r.data)
}

/** Just enough to populate the "new payment" client picker - duplicates the admin feature's
 * GET /api/client call rather than importing across features, matching this codebase's existing
 * tolerance for small duplication over cross-feature coupling (see AGENTS.md). */
export function getClientsForPicker() {
  return http
    .get<{ id: number; user: { email: string } }[]>('/api/client')
    .then((r): ClientSummaryDTO[] => r.data.map((c) => ({ id: c.id, email: c.user.email })))
}
