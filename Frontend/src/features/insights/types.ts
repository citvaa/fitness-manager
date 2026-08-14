export type InsightRating = 'EXCELLENT' | 'GOOD' | 'AVERAGE' | 'POOR'

export const RATING_LABEL: Record<InsightRating, string> = {
  EXCELLENT: 'Odlično',
  GOOD: 'Dobro',
  AVERAGE: 'Prosečno',
  POOR: 'Slabo',
}

// Same traffic-light convention as LiveFloorPlanPage's occupancy coloring (green/amber/red),
// extended with a distinct blue step for GOOD so all four ratings stay visually distinguishable.
export const RATING_COLOR: Record<InsightRating, string> = {
  EXCELLENT: '#22c55e',
  GOOD: '#38bdf8',
  AVERAGE: '#f59e0b',
  POOR: '#ef4444',
}

export interface RoomOccupancyInsight {
  roomName: string
  checkIns: number
  sharePercent: number
  rating: InsightRating
  comment: string
}

export interface SessionTypeInsight {
  sessionType: string
  paidAppointments: number
  sharePercent: number
  rating: InsightRating
  comment: string
}

export interface AttendanceInsight {
  distinctClients: number
  totalCheckIns: number
  avgCheckInDurationMinutes: number
  rating: InsightRating
  comment: string
}

export interface ManagerInsightsDTO {
  generatedAt: string // ISO LocalDateTime
  periodDays: number
  summary: string
  recommendations: string[]
  roomOccupancy: RoomOccupancyInsight[]
  sessionTypeBreakdown: SessionTypeInsight[]
  attendance: AttendanceInsight
}
