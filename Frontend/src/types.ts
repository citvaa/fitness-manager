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
export type EmploymentStatus='FULL_TIME'|'CONTRACT'|'FORMER_EMPLOYEE'
export interface UserAccount{id:number;email:string;roles:Role[];notificationPreference:'EMAIL'|'PUSH'|'BOTH';isActivated:boolean;registrationKey:string|null;registrationKeyValidity:string|null}
export interface TrainerProfile{id:number;user:UserAccount;employmentDate:string;birthYear:number;status:EmploymentStatus}
export interface ClientProfile{id:number;user:UserAccount}
export interface PageResponse<T>{content:T[];number:number;totalPages:number;totalElements:number}
export interface GymSchedule{id:number;day:string;openingTime:string;closingTime:string}
export interface Holiday{id:number;date:string;description:string}
export interface TrainerSchedule{id:number;trainer:TrainerProfile;date:string;startTime:string;endTime:string;status:'WORKING'|'HOLIDAY'|'SICK_LEAVE'|'VACATION'}
export interface SessionInfo{id:number;type:'INDIVIDUAL'|'GROUP';maxParticipants:number}
export interface Payment{id:number;client:ClientSummary;session:SessionInfo;paidAppointments:number;paymentDate:string}
export interface AppointmentSummary{id:number;date:string;startTime:string;endTime:string;session:SessionInfo;trainer:{id:number;email:string}|null;room:{id:number;name:string;type:string;capacity:number}|null;clients:ClientSummary[]}
export interface DailySchedule{date:string;appointments:AppointmentSummary[]}
