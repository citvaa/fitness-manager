import { api } from './client'
import type { AppointmentSummary } from '../types'

export const appointmentsApi = {
  mine: async () => (await api.get<AppointmentSummary[]>('/api/appointment/me')).data,
  withoutTrainer: async () => (await api.get<AppointmentSummary[]>('/api/appointment/without-trainer')).data,
  assign: async (id:number) => (await api.post<AppointmentSummary>(`/api/appointment/${id}/assign`)).data,
  unassign: async (id:number) => (await api.delete<AppointmentSummary>(`/api/appointment/${id}/unassign`)).data,
  available: async () => (await api.get<AppointmentSummary[]>('/api/appointment/available')).data,
  reserve: async (id:number) => (await api.post<AppointmentSummary>(`/api/appointment/${id}/reserve`)).data,
  cancel: async (id:number) => (await api.delete<AppointmentSummary>(`/api/appointment/${id}/cancel`)).data,
}
