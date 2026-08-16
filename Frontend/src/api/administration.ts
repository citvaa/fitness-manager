import { api } from './client'
import type { ClientProfile, EmploymentStatus, PageResponse, Role, TrainerProfile, UserAccount } from '../types'

export const usersApi={
  list:async(search='',page=0,role?:Role)=>(await api.get<PageResponse<UserAccount>>('/api/user',{params:{search,page,size:8,sortBy:'id',role}})).data,
  create:async(email:string,role:'MANAGER')=>(await api.post<UserAccount>('/api/user',{email,role})).data,
  update:async(id:number,email:string)=>(await api.put<UserAccount>(`/api/user/${id}`,{email})).data,
  remove:(id:number)=>api.delete(`/api/user/${id}`),
  addRole:(id:number,role:Role)=>api.post(`/api/user/${id}/role`,null,{params:{role}}),
  removeRole:(id:number,role:Role)=>api.delete(`/api/user/${id}/role`,{params:{role}}),
}
export const trainersApi={list:async()=>(await api.get<TrainerProfile[]>('/api/trainer')).data,create:async(data:{email:string;employmentDate:string;birthYear:number;status:EmploymentStatus})=>(await api.post<TrainerProfile>('/api/trainer',data)).data,update:(id:number,data:object)=>api.put(`/api/trainer/${id}`,data),remove:(id:number)=>api.delete(`/api/trainer/${id}`)}
export const clientsApi={list:async()=>(await api.get<ClientProfile[]>('/api/client')).data,create:async(email:string)=>(await api.post<ClientProfile>('/api/client',{email})).data,update:(id:number,email:string)=>api.put(`/api/client/${id}`,{email}),remove:(id:number)=>api.delete(`/api/client/${id}`)}
