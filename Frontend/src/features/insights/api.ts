import { http } from '../../lib/http'
import type { ManagerInsightsDTO } from './types'

export function getManagerInsights() {
  return http.get<ManagerInsightsDTO>('/api/insights/manager').then((r) => r.data)
}

export function refreshManagerInsights() {
  return http.post<ManagerInsightsDTO>('/api/insights/manager/refresh').then((r) => r.data)
}
