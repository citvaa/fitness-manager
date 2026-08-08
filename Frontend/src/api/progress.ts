import { api } from './client'
import type { AiInsight, ClientSummary, PersonalRecord, ProgressEntry, RecordUnit } from '../types'
export type ProgressEntryInput={entryDate:string;weightKg?:number;bodyFatPercent?:number;waistCm?:number;chestCm?:number;hipCm?:number;thighCm?:number;armCm?:number;notes?:string}
export type PersonalRecordInput={exerciseName:string;value:number;unit:RecordUnit;recordDate:string}
export const progressApi={
 trainerClients:async()=>(await api.get<ClientSummary[]>('/api/trainer/clients')).data,
 trainerEntries:async(id:number)=>(await api.get<ProgressEntry[]>(`/api/trainer/clients/${id}/progress/entries`)).data,
 trainerRecords:async(id:number)=>(await api.get<PersonalRecord[]>(`/api/trainer/clients/${id}/progress/records`)).data,
 trainerSummary:async(id:number,force=false)=>(await api.get<AiInsight>(`/api/trainer/clients/${id}/progress/summary`,{params:{force}})).data,
 createEntry:async(id:number,input:ProgressEntryInput)=>(await api.post<ProgressEntry>(`/api/trainer/clients/${id}/progress/entries`,input)).data,
 updateEntry:async(clientId:number,id:number,input:ProgressEntryInput)=>(await api.put<ProgressEntry>(`/api/trainer/clients/${clientId}/progress/entries/${id}`,input)).data,
 deleteEntry:async(clientId:number,id:number)=>api.delete(`/api/trainer/clients/${clientId}/progress/entries/${id}`),
 createRecord:async(id:number,input:PersonalRecordInput)=>(await api.post<PersonalRecord>(`/api/trainer/clients/${id}/progress/records`,input)).data,
 updateRecord:async(clientId:number,id:number,input:PersonalRecordInput)=>(await api.put<PersonalRecord>(`/api/trainer/clients/${clientId}/progress/records/${id}`,input)).data,
 deleteRecord:async(clientId:number,id:number)=>api.delete(`/api/trainer/clients/${clientId}/progress/records/${id}`),
 myEntries:async()=>(await api.get<ProgressEntry[]>('/api/client/progress/entries')).data,
 myRecords:async()=>(await api.get<PersonalRecord[]>('/api/client/progress/records')).data,
 mySummary:async()=>(await api.get<AiInsight>('/api/client/progress/summary')).data,
}
