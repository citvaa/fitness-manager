import { api } from './client'
import type { DailySchedule, Payment, PaymentStatus } from '../types'

export const paymentsApi = {
  list: async (clientId?: number) => (await api.get<Payment[]>('/api/payment', { params: clientId ? { clientId } : undefined })).data,
  mine: async () => (await api.get<Payment[]>('/api/payment/me')).data,
  myStatus: async () => (await api.get<PaymentStatus[]>('/api/payment/me/status')).data,
  status: async (clientId:number) => (await api.get<PaymentStatus[]>(`/api/payment/status/${clientId}`)).data,
  create: (value: { clientId: number; sessionId: number; paidAppointments: number; paymentDate: string }) => api.post<Payment>('/api/payment', value),
}

export const calendarApi = {
  day: async (date: string) => (await api.get<DailySchedule>('/api/calendar', { params: { date } })).data,
}
