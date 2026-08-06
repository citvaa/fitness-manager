import { api } from './client'
import type { Gym, OccupancySnapshot, Room, RoomDraft } from '../types'

export const gymApi = {
  gym: async () => (await api.get<Gym>('/api/gym')).data,
  createGym: async (body: Omit<Gym, 'id'>) => (await api.post<Gym>('/api/gym', body)).data,
  updateGym: async (id: number, body: Omit<Gym, 'id'>) => (await api.put<Gym>(`/api/gym/${id}`, body)).data,
  rooms: async () => (await api.get<Room[]>('/api/gym/rooms')).data,
  createRoom: async (body: RoomDraft) => (await api.post<Room>('/api/gym/rooms', body)).data,
  updateRoom: async (id: number, body: RoomDraft) => (await api.put<Room>(`/api/gym/rooms/${id}`, body)).data,
  deleteRoom: async (id: number) => { await api.delete(`/api/gym/rooms/${id}`) },
  occupancy: async () => (await api.get<OccupancySnapshot>('/api/gym/occupancy')).data,
}
