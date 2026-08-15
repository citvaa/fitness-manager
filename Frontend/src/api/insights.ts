import { api } from './client'
import type { ManagerInsight } from '../types'
export const insightsApi = { manager: async (force=false)=>(await api.get<ManagerInsight>('/api/manager/insights',{params:{force}})).data }
