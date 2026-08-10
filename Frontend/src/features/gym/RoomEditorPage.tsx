import { useEffect, useRef, useState } from 'react'
import { Stage, Layer, Rect, Transformer, Text, Group } from 'react-konva'
import type Konva from 'konva'
import { createRoom, deleteRoom, getGym, listRooms, updateRoom, upsertGym } from './api'
import type { GymDTO, RoomDTO, RoomType } from './types'
import { ROOM_TYPES, ROOM_TYPE_LABEL } from './types'

const PX_PER_UNIT = 20 // rendering scale: 1 geometry unit ("meter") = 20px on canvas
const CANVAS_WIDTH = 900
const CANVAS_HEIGHT = 600

// Smallest a room may be resized to (in geometry units/"meters") - small enough for a real
// closet-sized room, but large enough that the name label (e.g. "Recepcija") and the live-view
// occupancy count rendered on top of the rectangle always stay inside its bounds instead of
// spilling out. See AGENTS.md ("Upgrade: manager-testing fixes").
const MIN_ROOM_WIDTH_UNITS = 4
const MIN_ROOM_HEIGHT_UNITS = 2.5

const DEFAULT_COLOR = '#2f83fb'

function RoomShape({
  room,
  isSelected,
  onSelect,
  onChange,
}: {
  room: RoomDTO
  isSelected: boolean
  onSelect: () => void
  onChange: (patch: Partial<RoomDTO>) => void
}) {
  const shapeRef = useRef<Konva.Rect>(null)
  const trRef = useRef<Konva.Transformer>(null)

  useEffect(() => {
    if (isSelected && trRef.current && shapeRef.current) {
      trRef.current.nodes([shapeRef.current])
      trRef.current.getLayer()?.batchDraw()
    }
  }, [isSelected])

  return (
    <>
      <Group>
        <Rect
          ref={shapeRef}
          x={room.posX * PX_PER_UNIT}
          y={room.posY * PX_PER_UNIT}
          width={room.width * PX_PER_UNIT}
          height={room.height * PX_PER_UNIT}
          rotation={room.rotationDegrees}
          fill={(room.color ?? DEFAULT_COLOR) + '33'}
          stroke={room.color ?? DEFAULT_COLOR}
          strokeWidth={isSelected ? 3 : 2}
          cornerRadius={4}
          draggable
          onClick={onSelect}
          onTap={onSelect}
          onDragEnd={(e) => {
            onChange({
              posX: e.target.x() / PX_PER_UNIT,
              posY: e.target.y() / PX_PER_UNIT,
            })
          }}
          onTransformEnd={() => {
            const node = shapeRef.current
            if (!node) return
            const scaleX = node.scaleX()
            const scaleY = node.scaleY()
            node.scaleX(1)
            node.scaleY(1)
            onChange({
              posX: node.x() / PX_PER_UNIT,
              posY: node.y() / PX_PER_UNIT,
              width: Math.max(MIN_ROOM_WIDTH_UNITS, (node.width() * scaleX) / PX_PER_UNIT),
              height: Math.max(MIN_ROOM_HEIGHT_UNITS, (node.height() * scaleY) / PX_PER_UNIT),
              rotationDegrees: node.rotation(),
            })
          }}
        />
        <Text
          x={room.posX * PX_PER_UNIT + 6}
          y={room.posY * PX_PER_UNIT + 6}
          text={room.name}
          fontSize={13}
          fill="#e6e9f0"
          listening={false}
        />
      </Group>
      {isSelected && (
        <Transformer
          ref={trRef}
          rotateEnabled
          flipEnabled={false}
          boundBoxFunc={(oldBox, newBox) =>
            newBox.width < MIN_ROOM_WIDTH_UNITS * PX_PER_UNIT ||
            newBox.height < MIN_ROOM_HEIGHT_UNITS * PX_PER_UNIT
              ? oldBox
              : newBox
          }
        />
      )}
    </>
  )
}

export function RoomEditorPage() {
  const [gym, setGym] = useState<GymDTO | null>(null)
  const [gymName, setGymName] = useState('')
  const [rooms, setRooms] = useState<RoomDTO[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  async function reload() {
    const [gymData, roomList] = await Promise.all([getGym(), listRooms().catch(() => [])])
    setGym(gymData)
    setRooms(roomList)
    setLoading(false)
  }

  useEffect(() => {
    void reload()
  }, [])

  async function handleCreateGym() {
    if (!gymName.trim()) return
    setSaving(true)
    try {
      const created = await upsertGym({ name: gymName.trim() })
      setGym(created)
    } finally {
      setSaving(false)
    }
  }

  async function handleAddRoom() {
    if (!gym) return
    const created = await createRoom({
      gymId: gym.id,
      name: `Nova sala ${rooms.length + 1}`,
      type: 'WORKOUT_FLOOR',
      capacity: 10,
      posX: 2,
      posY: 2,
      width: 6,
      height: 4,
      rotationDegrees: 0,
      color: DEFAULT_COLOR,
    })
    setRooms((prev) => [...prev, created])
    setSelectedId(created.id)
  }

  async function persistPatch(room: RoomDTO, patch: Partial<RoomDTO>) {
    const next = { ...room, ...patch }
    setRooms((prev) => prev.map((r) => (r.id === room.id ? next : r)))
    await updateRoom(room.id, {
      name: next.name,
      type: next.type,
      capacity: next.capacity,
      posX: next.posX,
      posY: next.posY,
      width: next.width,
      height: next.height,
      rotationDegrees: next.rotationDegrees,
      color: next.color,
    })
  }

  async function handleDelete(id: number) {
    await deleteRoom(id)
    setRooms((prev) => prev.filter((r) => r.id !== id))
    if (selectedId === id) setSelectedId(null)
  }

  const selectedRoom = rooms.find((r) => r.id === selectedId) ?? null

  if (loading) {
    return <div className="p-8 text-slate-400">Učitavanje...</div>
  }

  if (!gym) {
    return (
      <div className="flex min-h-screen items-center justify-center p-8">
        <div className="w-full max-w-sm rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
          <h2 className="mb-1 text-lg font-semibold text-slate-100">Podesi teretanu</h2>
          <p className="mb-4 text-sm text-slate-400">
            Pre crtanja plana potrebno je da postoji jedan Gym zapis (jedna instalacija).
          </p>
          <input
            value={gymName}
            onChange={(e) => setGymName(e.target.value)}
            placeholder="Naziv teretane"
            className="mb-3 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-brand-500"
          />
          <button
            onClick={handleCreateGym}
            disabled={saving}
            className="w-full rounded-lg bg-brand-600 px-3 py-2 font-medium text-white hover:bg-brand-500 disabled:opacity-60"
          >
            {saving ? 'Čuvanje...' : 'Sačuvaj'}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-full">
      <div className="flex-1 overflow-auto p-6">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h1 className="text-lg font-semibold text-slate-100">Editor sala — {gym.name}</h1>
            <p className="text-sm text-slate-500">
              Povuci salu da je pomjeriš, uhvati za ugao da promijeniš veličinu/rotaciju.
            </p>
          </div>
          <button
            onClick={handleAddRoom}
            className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-500"
          >
            + Nova sala
          </button>
        </div>

        <div className="relative inline-block rounded-xl border border-slate-800 bg-slate-900/40 p-2">
          <Stage
            width={CANVAS_WIDTH}
            height={CANVAS_HEIGHT}
            onMouseDown={(e) => {
              if (e.target === e.target.getStage()) setSelectedId(null)
            }}
            className="rounded-lg bg-[radial-gradient(circle_at_1px_1px,#1e293b_1px,transparent_0)] [background-size:20px_20px]"
          >
            <Layer>
              {rooms.map((room) => (
                <RoomShape
                  key={room.id}
                  room={room}
                  isSelected={room.id === selectedId}
                  onSelect={() => setSelectedId(room.id)}
                  onChange={(patch) => void persistPatch(room, patch)}
                />
              ))}
            </Layer>
          </Stage>
          {rooms.length === 0 && (
            <p className="pointer-events-none absolute inset-2 flex items-center justify-center text-sm text-slate-500">
              Još nema sala — klikni „+ Nova sala" da dodaš prvu.
            </p>
          )}
        </div>
      </div>

      <aside className="w-80 shrink-0 border-l border-slate-800 bg-slate-900/40 p-5">
        <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-400">
          Sale ({rooms.length})
        </h2>
        {rooms.length === 0 && (
          <p className="mb-6 text-sm text-slate-500">Još nema sala u ovoj teretani.</p>
        )}
        <ul className="mb-6 space-y-1">
          {rooms.map((room) => (
            <li key={room.id}>
              <button
                onClick={() => setSelectedId(room.id)}
                className={`w-full rounded-lg px-3 py-2 text-left text-sm transition ${
                  room.id === selectedId
                    ? 'bg-slate-800 text-white'
                    : 'text-slate-400 hover:bg-slate-800/60'
                }`}
              >
                {room.name}{' '}
                <span className="text-xs text-slate-500">({ROOM_TYPE_LABEL[room.type]})</span>
              </button>
            </li>
          ))}
        </ul>

        {selectedRoom && (
          <div className="space-y-3 rounded-xl border border-slate-800 bg-slate-950 p-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold text-slate-200">Podešavanja sale</h3>
              <button
                onClick={() => handleDelete(selectedRoom.id)}
                className="text-xs text-red-400 hover:text-red-300"
              >
                Obriši
              </button>
            </div>

            <label className="block text-xs text-slate-400">
              Naziv
              <input
                value={selectedRoom.name}
                onChange={(e) => void persistPatch(selectedRoom, { name: e.target.value })}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
              />
            </label>

            <label className="block text-xs text-slate-400">
              Tip
              <select
                value={selectedRoom.type}
                onChange={(e) =>
                  void persistPatch(selectedRoom, { type: e.target.value as RoomType })
                }
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
              >
                {ROOM_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {ROOM_TYPE_LABEL[t]}
                  </option>
                ))}
              </select>
            </label>

            <label className="block text-xs text-slate-400">
              Kapacitet
              <input
                type="number"
                min={1}
                value={selectedRoom.capacity}
                onChange={(e) =>
                  void persistPatch(selectedRoom, { capacity: Number(e.target.value) })
                }
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
              />
            </label>

            <label className="block text-xs text-slate-400">
              Boja
              <input
                type="color"
                value={selectedRoom.color ?? DEFAULT_COLOR}
                onChange={(e) => void persistPatch(selectedRoom, { color: e.target.value })}
                className="mt-1 h-8 w-full rounded-lg border border-slate-700 bg-slate-900"
              />
            </label>

            <div className="grid grid-cols-2 gap-2 text-xs text-slate-500">
              <span>Š: {selectedRoom.width.toFixed(1)}m</span>
              <span>V: {selectedRoom.height.toFixed(1)}m</span>
              <span>X: {selectedRoom.posX.toFixed(1)}m</span>
              <span>Y: {selectedRoom.posY.toFixed(1)}m</span>
            </div>
          </div>
        )}
      </aside>
    </div>
  )
}
