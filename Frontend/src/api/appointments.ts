import { api } from './client'
import type { AppointmentSummary, SessionInfo } from '../types'

export type CreateAppointmentInput={date:string;startTime:string;endTime:string;sessionId:number;roomId?:number;trainerId?:number;clientIds?:number[]}

export const appointmentsApi = {
  sessions: async () => (await api.get<SessionInfo[]>('/api/appointment/sessions')).data,
  create: async (input:CreateAppointmentInput) => (await api.post<AppointmentSummary>('/api/appointment',input)).data,
  addTrainer: async (id:number,trainerId:number) => (await api.post<AppointmentSummary>(`/api/appointment/${id}/add-trainer`,null,{params:{trainerId}})).data,
  removeTrainer: async (id:number) => (await api.delete<AppointmentSummary>(`/api/appointment/${id}/remove-trainer`)).data,
  addClients: async (id:number,clientIds:number[]) => (await api.post<AppointmentSummary>(`/api/appointment/${id}/add-clients`,null,{params:{clientIds:clientIds.join(',')}})).data,
  removeClient: async (id:number,clientId:number) => (await api.delete<AppointmentSummary>(`/api/appointment/${id}/remove-client`,{params:{clientId}})).data,
  mine: async () => (await api.get<AppointmentSummary[]>('/api/appointment/me')).data,
  withoutTrainer: async () => (await api.get<AppointmentSummary[]>('/api/appointment/without-trainer')).data,
  assign: async (id:number) => (await api.post<AppointmentSummary>(`/api/appointment/${id}/assign`)).data,
  unassign: async (id:number) => (await api.delete<AppointmentSummary>(`/api/appointment/${id}/unassign`)).data,
  available: async () => (await api.get<AppointmentSummary[]>('/api/appointment/available')).data,
  reserve: async (id:number) => (await api.post<AppointmentSummary>(`/api/appointment/${id}/reserve`)).data,
  cancel: async (id:number) => (await api.delete<AppointmentSummary>(`/api/appointment/${id}/cancel`)).data,
}
