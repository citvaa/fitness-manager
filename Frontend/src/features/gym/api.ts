import { http } from '../../lib/http'
import type {
  CreateRoomRequest,
  GymDTO,
  RoomDTO,
  RoomOccupancyDTO,
  UpdateRoomRequest,
  UpsertGymRequest,
} from './types'

export async function getGym(): Promise<GymDTO | null> {
  try {
    const res = await http.get<GymDTO>('/api/gym')
    return res.data
  } catch {
    // No gym configured yet - GymServiceImpl.upsertGym is what creates it.
    return null
  }
}

export function upsertGym(request: UpsertGymRequest) {
  return http.put<GymDTO>('/api/gym', request).then((r) => r.data)
}

export function listRooms() {
  return http.get<RoomDTO[]>('/api/gym/room').then((r) => r.data)
}

export function createRoom(request: CreateRoomRequest) {
  return http.post<RoomDTO>('/api/gym/room', request).then((r) => r.data)
}

export function updateRoom(id: number, request: UpdateRoomRequest) {
  return http.put<RoomDTO>(`/api/gym/room/${id}`, request).then((r) => r.data)
}

export function deleteRoom(id: number) {
  return http.delete(`/api/gym/room/${id}`)
}

export function getOccupancy() {
  return http.get<RoomOccupancyDTO[]>('/api/gym/occupancy').then((r) => r.data)
}
