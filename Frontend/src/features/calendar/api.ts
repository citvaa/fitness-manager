import { http } from '../../lib/http'
import type { DailyScheduleDTO, RoomSummaryDTO, TrainerSummaryDTO } from './types'

/** GET /api/calendar - now MANAGER/TRAINER only, see AGENTS.md "Known issues"/"Upgrade: Faza 6 decisions". */
export function getDailySchedule(date: string) {
  return http.get<DailyScheduleDTO>('/api/calendar', { params: { date } }).then((r) => r.data)
}

/** Just enough to populate the trainer filter dropdown next to the calendar - duplicates
 * features/admin/api.ts's getTrainers() shape rather than importing across features, matching
 * this codebase's existing tolerance for small duplication over cross-feature coupling. */
export function getTrainersForFilter() {
  return http
    .get<{ id: number; user: { email: string } }[]>('/api/trainer')
    .then((r): TrainerSummaryDTO[] => r.data.map((t) => ({ id: t.id, email: t.user.email })))
}

/** Same reasoning as getTrainersForFilter() above, for the room filter. */
export function getRoomsForFilter() {
  return http
    .get<{ id: number; name: string }[]>('/api/gym/room')
    .then((r): RoomSummaryDTO[] => r.data.map((room) => ({ id: room.id, name: room.name })))
}
