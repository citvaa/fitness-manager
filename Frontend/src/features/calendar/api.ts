import { http } from '../../lib/http'
import type { DailyScheduleDTO } from './types'

/** GET /api/calendar - now MANAGER/TRAINER only, see AGENTS.md "Known issues"/"Upgrade: Faza 6 decisions". */
export function getDailySchedule(date: string) {
  return http.get<DailyScheduleDTO>('/api/calendar', { params: { date } }).then((r) => r.data)
}
