export type RoomType =
  | 'WORKOUT_FLOOR'
  | 'STUDIO'
  | 'POOL'
  | 'LOCKER_ROOM'
  | 'RECEPTION'
  | 'OFFICE'
  | 'OTHER'

export const ROOM_TYPES: RoomType[] = [
  'WORKOUT_FLOOR',
  'STUDIO',
  'POOL',
  'LOCKER_ROOM',
  'RECEPTION',
  'OFFICE',
  'OTHER',
]

export const ROOM_TYPE_LABEL: Record<RoomType, string> = {
  WORKOUT_FLOOR: 'Teretana',
  STUDIO: 'Studio',
  POOL: 'Bazen',
  LOCKER_ROOM: 'Svlačionica',
  RECEPTION: 'Recepcija',
  OFFICE: 'Kancelarija',
  OTHER: 'Ostalo',
}

// Used on the live floor-plan tiles (LiveFloorPlanPage) next to the room
// name - plain emoji, not an icon-font/SVG set, to keep this dependency-free.
export const ROOM_TYPE_ICON: Record<RoomType, string> = {
  WORKOUT_FLOOR: '🏋️',
  STUDIO: '🧘',
  POOL: '🏊',
  LOCKER_ROOM: '🧺',
  RECEPTION: '🛎️',
  OFFICE: '💼',
  OTHER: '📍',
}

export interface GymSummary {
  id: number
  name: string
}

export interface GymDTO {
  id: number
  name: string
  address: string | null
  contactEmail: string | null
  contactPhone: string | null
  logoUrl: string | null
  primaryColor: string | null
  timezone: string | null
}

export interface RoomDTO {
  id: number
  gym: GymSummary
  name: string
  type: RoomType
  capacity: number
  posX: number
  posY: number
  width: number
  height: number
  rotationDegrees: number
  color: string | null
}

export interface UpsertGymRequest {
  name: string
  address?: string | null
  contactEmail?: string | null
  contactPhone?: string | null
  logoUrl?: string | null
  primaryColor?: string | null
  timezone?: string | null
}

export interface CreateRoomRequest {
  gymId: number
  name: string
  type: RoomType
  capacity: number
  posX: number
  posY: number
  width: number
  height: number
  rotationDegrees: number
  color?: string | null
}

export type UpdateRoomRequest = Omit<CreateRoomRequest, 'gymId'>

export interface RoomOccupancyDTO {
  roomId: number
  roomName: string
  capacity: number
  checkedInCount: number
  appointmentOccupantCount: number
  totalOccupancy: number
  occupancyPercent: number
  atCapacity: boolean
}
