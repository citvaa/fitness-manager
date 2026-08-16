import type { GymSchedule, Holiday } from '../types'

const weekdayNumber: Record<string, number> = {
  SUNDAY: 0, MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3,
  THURSDAY: 4, FRIDAY: 5, SATURDAY: 6,
}

export function gymClosure(schedules: GymSchedule[], holidays: Holiday[]) {
  const covered = new Set(schedules
    .filter(row => row.openingTime < row.closingTime)
    .map(row => weekdayNumber[row.day])
    .filter(day => day !== undefined))
  return {
    mutedDates: new Set(holidays.map(holiday => holiday.date)),
    mutedWeekdays: new Set(Array.from({ length: 7 }, (_, day) => day).filter(day => !covered.has(day))),
  }
}

export function isGymClosed(date: string, mutedDates: Set<string>, mutedWeekdays: Set<number>) {
  return mutedDates.has(date) || mutedWeekdays.has(new Date(`${date}T12:00:00`).getDay())
}
