export type Role = 'MANAGER' | 'TRAINER' | 'CLIENT'
export type RoomType = 'CARDIO' | 'WEIGHTS' | 'GROUP_STUDIO' | 'FUNCTIONAL' | 'LOCKER_ROOM' | 'OTHER'

export interface AuthResponse {
  accessToken: string
  accessTokenExpirationTime: string
  refreshToken: string
  refreshTokenExpirationTime: string
}

export interface JwtClaims {
  sub: string
  email?: string
  roles?: Role[]
  exp: number
}

export interface Gym {
  id: number
  name: string
  address: string
  phone: string
  email: string
  logoUrl: string | null
  brandColor: string
  timezone: string
}

export interface Room {
  id: number
  gym: Gym
  name: string
  type: RoomType
  capacity: number
  posX: number
  posY: number
  width: number
  height: number
  rotationDegrees: number
}

export type RoomDraft = Omit<Room, 'id' | 'gym'>

export interface RoomOccupancy {
  roomId: number
  roomName: string
  capacity: number
  manualCheckIns: number
  scheduledParticipants: number
  totalOccupancy: number
}

export interface OccupancySnapshot {
  generatedAt: string
  rooms: RoomOccupancy[]
}

export interface AiInsight { text: string; model: string; generatedAt: string }

export interface ClientSummary { id:number; email:string }
export type RecordUnit='KG'|'LB'|'REPS'|'SECONDS'|'MINUTES'|'METERS'|'KM'
export interface ProgressEntry { id:number; client:ClientSummary; entryDate:string; weightKg:number|null; bodyFatPercent:number|null; waistCm:number|null; chestCm:number|null; hipCm:number|null; thighCm:number|null; armCm:number|null; notes:string|null }
export interface PersonalRecord { id:number; client:ClientSummary; exerciseName:string; value:number; unit:RecordUnit; recordDate:string }
