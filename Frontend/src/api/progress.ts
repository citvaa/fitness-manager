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
 createRecord:async(id:number,input:PersonalRecordInput)=>(await api.post<PersonalRecord>(`/api/trainer/clients/${id}/progress/records`,input)).data,
 myEntries:async()=>(await api.get<ProgressEntry[]>('/api/client/progress/entries')).data,
 myRecords:async()=>(await api.get<PersonalRecord[]>('/api/client/progress/records')).data,
 mySummary:async()=>(await api.get<AiInsight>('/api/client/progress/summary')).data,
}
