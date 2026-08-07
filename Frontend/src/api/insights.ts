import { api } from './client'
import type { AiInsight } from '../types'
export const insightsApi = { manager: async (force=false)=>(await api.get<AiInsight>('/api/manager/insights',{params:{force}})).data }
