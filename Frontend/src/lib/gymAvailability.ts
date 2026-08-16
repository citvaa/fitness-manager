import { http } from './http'

interface HolidayLite {
  date: string
  description: string
}

interface GymScheduleLite {
  day: string // backend DayOfWeek enum name, e.g. "MONDAY"
  openingTime: string
  closingTime: string
}

/** Both endpoints are open to any authenticated role server-side (see
 * HolidayController/GymScheduleController) - duplicated fetch wrappers here rather than importing
 * from features/admin/api.ts, matching this codebase's existing tolerance for small duplication
 * over cross-feature coupling (see AGENTS.md "Conventions"). */
function getHolidaysLite() {
  return http.get<HolidayLite[]>('/api/schedule/holiday').then((r) => r.data)
}

function getGymScheduleLite() {
  return http.get<GymScheduleLite[]>('/api/schedule/gym').then((r) => r.data)
}

const JS_DAY_TO_BACKEND_DAY = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']

/**
 * Fetches holidays + gym opening hours once and returns a `getMutedReason`-shaped function
 * (see MonthCalendar) that flags a date as gym-wide unavailable - either a specific holiday
 * (using the real `Holiday.description` as the reason text, same field
 * AppointmentServiceImpl#validateNotHoliday already treats as the source of truth) or a weekday
 * with no `GymSchedule` row at all (mirrors AppointmentServiceImpl#validateGymSchedule's own
 * "radno vreme nije definisano" check - a day only reads as "closed" when no schedule row exists
 * for it, never inferred from opening/closing times). Used by every MonthCalendar call site (gym
 * closure applies everywhere, unlike trainer-specific unavailability). See AGENTS.md "Upgrade:
 * MonthCalendar unavailability decisions".
 */
export async function buildGymMutedReason(): Promise<(isoDate: string) => string | null> {
  const [holidays, gymSchedule] = await Promise.all([getHolidaysLite(), getGymScheduleLite()])
  const holidayByDate = new Map(holidays.map((h) => [h.date, h.description]))
  const daysWithSchedule = new Set(gymSchedule.map((s) => s.day))

  return (isoDate: string) => {
    const holidayReason = holidayByDate.get(isoDate)
    if (holidayReason) return `Praznik: ${holidayReason}`

    const weekday = JS_DAY_TO_BACKEND_DAY[new Date(isoDate + 'T00:00:00').getDay()]
    if (!daysWithSchedule.has(weekday)) return 'Teretana ne radi ovim danom u nedelji.'

    return null
  }
}
