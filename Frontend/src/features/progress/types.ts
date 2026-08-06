export type RecordUnit = 'KG' | 'LB' | 'REPS' | 'SECONDS' | 'MINUTES' | 'METERS' | 'KM'

export const RECORD_UNITS: RecordUnit[] = ['KG', 'LB', 'REPS', 'SECONDS', 'MINUTES', 'METERS', 'KM']

export const RECORD_UNIT_LABEL: Record<RecordUnit, string> = {
  KG: 'kg',
  LB: 'lb',
  REPS: 'ponavljanja',
  SECONDS: 's',
  MINUTES: 'min',
  METERS: 'm',
  KM: 'km',
}

export interface ClientSummaryDTO {
  id: number
  email: string
}

export interface ClientProgressEntryDTO {
  id: number
  client: ClientSummaryDTO
  entryDate: string // ISO LocalDate
  weightKg: number | null
  bodyFatPercent: number | null
  waistCm: number | null
  chestCm: number | null
  hipCm: number | null
  thighCm: number | null
  armCm: number | null
  notes: string | null
}

export interface ClientPersonalRecordDTO {
  id: number
  client: ClientSummaryDTO
  exerciseName: string
  value: number
  unit: RecordUnit
  recordDate: string // ISO LocalDate
  notes: string | null
}

export interface ClientProgressInsightDTO {
  clientId: number
  narrative: string
  generatedAt: string // ISO LocalDateTime
}

export interface CreateProgressEntryRequest {
  clientId: number
  entryDate: string
  weightKg?: number | null
  bodyFatPercent?: number | null
  waistCm?: number | null
  chestCm?: number | null
  hipCm?: number | null
  thighCm?: number | null
  armCm?: number | null
  notes?: string | null
}

export interface CreatePersonalRecordRequest {
  clientId: number
  exerciseName: string
  value: number
  unit: RecordUnit
  recordDate: string
  notes?: string | null
}
