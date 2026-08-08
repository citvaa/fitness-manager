import { http } from '../../lib/http'
import type {
  ClientPersonalRecordDTO,
  ClientProgressEntryDTO,
  ClientProgressInsightDTO,
  ClientSummaryDTO,
  CreatePersonalRecordRequest,
  CreateProgressEntryRequest,
} from './types'

// Trainer-facing (own clients only, enforced backend-side by TrainerClientAccessGuard).
export function getMyClients() {
  return http.get<ClientSummaryDTO[]>('/api/trainer/me/clients').then((r) => r.data)
}

export function getEntriesForClient(clientId: number) {
  return http.get<ClientProgressEntryDTO[]>(`/api/progress/entry/client/${clientId}`).then((r) => r.data)
}

export function createEntry(request: CreateProgressEntryRequest) {
  return http.post<ClientProgressEntryDTO>('/api/progress/entry', request).then((r) => r.data)
}

// Correcting/removing an existing entry - added in Faza 9 (previously entry recording had no
// way to fix a typo or remove a bad measurement, on the backend or the UI - see AGENTS.md
// "Upgrade: Faza 9 decisions"). Same MANAGER/TRAINER + trainer-has-trained-this-client
// authorization as create().
export function updateEntry(id: number, request: CreateProgressEntryRequest) {
  return http.put<ClientProgressEntryDTO>(`/api/progress/entry/${id}`, request).then((r) => r.data)
}

export function deleteEntry(id: number) {
  return http.delete(`/api/progress/entry/${id}`)
}

export function getRecordsForClient(clientId: number) {
  return http.get<ClientPersonalRecordDTO[]>(`/api/progress/record/client/${clientId}`).then((r) => r.data)
}

export function createRecord(request: CreatePersonalRecordRequest) {
  return http.post<ClientPersonalRecordDTO>('/api/progress/record', request).then((r) => r.data)
}

export function updateRecord(id: number, request: CreatePersonalRecordRequest) {
  return http.put<ClientPersonalRecordDTO>(`/api/progress/record/${id}`, request).then((r) => r.data)
}

export function deleteRecord(id: number) {
  return http.delete(`/api/progress/record/${id}`)
}

export function getInsightForClient(clientId: number) {
  return http.get<ClientProgressInsightDTO>(`/api/progress/insight/client/${clientId}`).then((r) => r.data)
}

// Client-facing ("my own data").
export function getMyEntries() {
  return http.get<ClientProgressEntryDTO[]>('/api/progress/entry/me').then((r) => r.data)
}

export function getMyRecords() {
  return http.get<ClientPersonalRecordDTO[]>('/api/progress/record/me').then((r) => r.data)
}

export function getMyInsight() {
  return http.get<ClientProgressInsightDTO>('/api/progress/insight/me').then((r) => r.data)
}
