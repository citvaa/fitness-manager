import { useEffect, useState } from 'react'
import { listRooms } from './api'
import { useOccupancySocket } from './useOccupancySocket'
import type { RoomDTO, RoomOccupancyDTO } from './types'
import { ROOM_TYPE_ICON, ROOM_TYPE_LABEL } from './types'
import { useAnimatedNumber } from '../../lib/useAnimatedNumber'
import clsx from 'clsx'

const PX_PER_UNIT = 20

// A tile glows/breathes once it's this close to capacity, not only once it
// hits 100% - "blizu punog kapaciteta" from the design brief, not just
// "puno".
const NEAR_CAPACITY_PERCENT = 85

function occupancyColor(percent: number, atCapacity: boolean) {
  if (atCapacity || percent >= 100) return { bg: 'rgba(239,68,68,0.35)', ring: '#ef4444' }
  if (percent >= 60) return { bg: 'rgba(245,158,11,0.30)', ring: '#f59e0b' }
  if (percent > 0) return { bg: 'rgba(34,197,94,0.28)', ring: '#22c55e' }
  return { bg: 'rgba(100,116,139,0.15)', ring: '#334155' }
}

function RoomTile({ room, occ }: { room: RoomDTO; occ: RoomOccupancyDTO | undefined }) {
  const percent = occ?.occupancyPercent ?? 0
  const atCapacity = occ?.atCapacity ?? false
  const isNearCapacity = atCapacity || percent >= NEAR_CAPACITY_PERCENT
  const { bg, ring } = occupancyColor(percent, atCapacity)
  const count = occ?.totalOccupancy ?? 0

  // Tween both the headcount and the percent so they visibly count up/down
  // on every WebSocket update instead of snapping straight to the new value.
  const animatedCount = useAnimatedNumber(count)
  const animatedPercent = useAnimatedNumber(percent)
  const barWidth = Math.min(Math.max(animatedPercent, 0), 100)

  return (
    <div
      className={clsx(
        'absolute flex flex-col justify-between rounded-xl border-2 p-3 shadow-lg backdrop-blur-sm transition-colors transition-shadow duration-700 ease-out',
        isNearCapacity && 'glow-pulse',
      )}
      style={
        {
          left: room.posX * PX_PER_UNIT,
          top: room.posY * PX_PER_UNIT,
          width: room.width * PX_PER_UNIT,
          height: room.height * PX_PER_UNIT,
          transform: `rotate(${room.rotationDegrees}deg)`,
          backgroundColor: bg,
          borderColor: ring,
          '--glow-color': ring,
        } as React.CSSProperties
      }
    >
      <div>
        <p className="flex items-center gap-1.5 truncate text-sm font-semibold text-slate-100">
          <span aria-hidden>{ROOM_TYPE_ICON[room.type]}</span>
          <span className="truncate">{room.name}</span>
        </p>
        <p className="text-[11px] uppercase tracking-wide text-slate-400">
          {ROOM_TYPE_LABEL[room.type]}
        </p>
      </div>

      <div>
        <div className="mb-1.5 h-1.5 w-full overflow-hidden rounded-full bg-black/30">
          <div
            className="h-full rounded-full transition-[width] duration-700 ease-out"
            style={{ width: `${barWidth}%`, backgroundColor: ring }}
          />
        </div>
        <div className="flex items-end justify-between">
          <span
            className="rounded-full px-2 py-0.5 text-xs font-bold tabular-nums text-white transition-colors duration-700"
            style={{ backgroundColor: ring }}
          >
            {Math.round(animatedCount)}/{room.capacity}
          </span>
          <span className="text-xs tabular-nums text-slate-400">
            {Math.round(animatedPercent)}%
          </span>
        </div>
      </div>
    </div>
  )
}

export function LiveFloorPlanPage() {
  const [rooms, setRooms] = useState<RoomDTO[]>([])
  const { occupancy, connected } = useOccupancySocket()

  useEffect(() => {
    void listRooms().then(setRooms)
  }, [])

  const occByRoomId = new Map(occupancy.map((o) => [o.roomId, o]))
  const totalPeople = occupancy.reduce((sum, o) => sum + o.totalOccupancy, 0)
  const totalCapacity = rooms.reduce((sum, r) => sum + r.capacity, 0)

  const maxX = Math.max(200, ...rooms.map((r) => (r.posX + r.width) * PX_PER_UNIT + 40))
  const maxY = Math.max(200, ...rooms.map((r) => (r.posY + r.height) * PX_PER_UNIT + 40))

  return (
    <div className="p-6">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-lg font-semibold text-slate-100">Plan teretane — uživo</h1>
          <p className="text-sm text-slate-500">
            Boja, traka i broj ljudi po sali ažuriraju se u realnom vremenu.
          </p>
        </div>

        <div className="flex items-center gap-4">
          <div className="rounded-xl border border-slate-800 bg-slate-900/60 px-4 py-2 text-right">
            <p className="text-xs text-slate-500">Trenutno u teretani</p>
            <p className="text-lg font-bold text-slate-100">
              {totalPeople}
              <span className="text-sm font-normal text-slate-500"> / {totalCapacity}</span>
            </p>
          </div>
          <div className="flex items-center gap-2 rounded-full border border-slate-800 bg-slate-900/60 px-3 py-1.5 text-xs">
            <span
              className={clsx(
                'h-2 w-2 rounded-full',
                connected ? 'bg-emerald-400 animate-pulse' : 'bg-slate-600',
              )}
            />
            <span className={connected ? 'text-emerald-300' : 'text-slate-500'}>
              {connected ? 'Uživo' : 'Povezivanje...'}
            </span>
          </div>
        </div>
      </div>

      <div className="mb-4 flex gap-4 text-xs text-slate-400">
        <Legend color="#334155" label="Slobodno" />
        <Legend color="#22c55e" label="Nizak broj ljudi" />
        <Legend color="#f59e0b" label="Popunjeno &gt;60%" />
        <Legend color="#ef4444" label="Blizu/pun kapacitet" glow />
      </div>

      <div
        className="relative rounded-2xl border border-slate-800 bg-[radial-gradient(circle_at_1px_1px,#1e293b_1px,transparent_0)] [background-size:20px_20px]"
        style={{ width: maxX, height: maxY }}
      >
        {rooms.length === 0 && (
          <p className="absolute inset-0 flex items-center justify-center text-sm text-slate-500">
            Nema sala — dodaj sale u Editoru sala.
          </p>
        )}
        {rooms.map((room) => (
          <RoomTile key={room.id} room={room} occ={occByRoomId.get(room.id)} />
        ))}
      </div>
    </div>
  )
}

function Legend({ color, label, glow }: { color: string; label: string; glow?: boolean }) {
  return (
    <span className="flex items-center gap-1.5">
      <span
        className={clsx('h-2.5 w-2.5 rounded-full', glow && 'glow-pulse')}
        style={{ backgroundColor: color, '--glow-color': color } as React.CSSProperties}
      />
      {label}
    </span>
  )
}
