import { useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import { LoadingIndicator } from '../../components/LoadingIndicator'
import { checkInClient, checkOutClient, getActiveCheckIn } from './api'
import type { AppointmentDTO, RoomCheckInDTO } from './types'

function extractErrorMessage(err: unknown, fallback: string): string {
  if (isAxiosError(err) && typeof err.response?.data?.message === 'string') {
    return err.response.data.message
  }
  return fallback
}

/**
 * TRAINER "Započni trening" action - per-client Check-in/Check-out for one specific appointment's
 * roster, backed by the pre-existing `/api/gym/room/{roomId}/check-in`/`check-out` endpoints (see
 * AGENTS.md "Upgrade: trainer check-in decisions") - those existed fully wired since an earlier
 * round but had no frontend caller at all. Independent of the Appointment model itself (check-in
 * is a room concept, not an appointment one) - this panel is just a convenient entry point scoped
 * to "the clients on this specific appointment" rather than a new domain relationship.
 *
 * A client's check-in status is global (at most one active check-in anywhere, enforced server-
 * side), not appointment-scoped, so `getActiveCheckIn` is queried per client on mount/refresh
 * rather than assumed from this appointment alone - a client could already be checked into a
 * different room (e.g. mid-workout before this session) and should show as checked-in here too,
 * with a check-out that correctly targets whichever check-in they actually have open.
 *
 * No manual WebSocket wiring needed here for the live floor-plan view to reflect these changes -
 * `RoomCheckInServiceImpl.checkIn()`/`checkOut()` already broadcast the updated occupancy snapshot
 * on every call.
 */
export function ClientCheckInPanel({ appointment }: { appointment: AppointmentDTO }) {
  const [statuses, setStatuses] = useState<Record<number, RoomCheckInDTO | null>>({})
  const [loading, setLoading] = useState(true)
  const [busyClientId, setBusyClientId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function reload() {
    setLoading(true)
    try {
      const entries = await Promise.all(
        appointment.clients.map(async (c) => [c.id, await getActiveCheckIn(c.id)] as const),
      )
      setStatuses(Object.fromEntries(entries))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appointment.id])

  async function handleCheckIn(clientId: number) {
    if (!appointment.room) return
    setError(null)
    setBusyClientId(clientId)
    try {
      await checkInClient(appointment.room.id, clientId)
      await reload()
    } catch (err) {
      setError(extractErrorMessage(err, 'Check-in nije uspeo.'))
    } finally {
      setBusyClientId(null)
    }
  }

  async function handleCheckOut(checkInId: number, clientId: number) {
    setError(null)
    setBusyClientId(clientId)
    try {
      await checkOutClient(checkInId)
      await reload()
    } catch (err) {
      setError(extractErrorMessage(err, 'Check-out nije uspeo.'))
    } finally {
      setBusyClientId(null)
    }
  }

  if (!appointment.room) {
    return (
      <p className="mt-2 text-xs text-amber-400">
        Termin nema dodeljenu sobu - check-in zahteva sobu.
      </p>
    )
  }

  if (appointment.clients.length === 0) {
    return <p className="mt-2 text-xs text-slate-500">Nema prijavljenih klijenata na ovaj termin.</p>
  }

  return (
    <div className="mt-2 space-y-1">
      {error && <p className="mb-1 text-xs text-red-400">{error}</p>}
      {loading ? (
        <LoadingIndicator className="text-xs text-slate-500" />
      ) : (
        appointment.clients.map((c) => {
          const activeCheckIn = statuses[c.id]
          const busy = busyClientId === c.id
          // A check-in in a DIFFERENT room than this appointment's own is still "active" (a
          // client is globally limited to one check-in at a time) - checked out from wherever
          // it actually is, not assumed to be this room.
          return (
            <div
              key={c.id}
              className="flex items-center justify-between rounded-lg bg-slate-950/60 px-3 py-1.5 text-xs"
            >
              <span className="text-slate-300">{c.email}</span>
              {activeCheckIn ? (
                <button
                  onClick={() => handleCheckOut(activeCheckIn.id, c.id)}
                  disabled={busy}
                  className="rounded-lg border border-red-900/50 px-2 py-0.5 text-xs text-red-300 hover:bg-red-950/40 disabled:opacity-60"
                >
                  {busy ? '...' : 'Check-out'}
                </button>
              ) : (
                <button
                  onClick={() => handleCheckIn(c.id)}
                  disabled={busy}
                  className="rounded-lg bg-brand-600 px-2 py-0.5 text-xs font-medium text-white hover:bg-brand-500 disabled:opacity-60"
                >
                  {busy ? '...' : 'Check-in'}
                </button>
              )}
            </div>
          )
        })
      )}
    </div>
  )
}
