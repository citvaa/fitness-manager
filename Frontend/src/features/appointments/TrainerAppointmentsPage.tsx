import { useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import {
  assignSelfToAppointment,
  getAppointmentsWithoutTrainer,
  getMyAppointmentsAsTrainer,
  unassignSelfFromAppointment,
} from './api'
import type { AppointmentDTO } from './types'

const SESSION_TYPE_LABEL: Record<string, string> = { INDIVIDUAL: 'Individualni', GROUP: 'Grupni' }

function extractErrorMessage(err: unknown, fallback: string): string {
  if (isAxiosError(err) && typeof err.response?.data?.message === 'string') {
    return err.response.data.message
  }
  return fallback
}

function isPast(a: AppointmentDTO) {
  return `${a.date}T${a.endTime}` < new Date().toISOString()
}

function isUpcoming(a: AppointmentDTO) {
  return !isPast(a)
}

/**
 * TRAINER self-service scheduling - see AGENTS.md "Upgrade: Faza 7 decisions". This is the
 * "marketplace" side of the appointment model: a trainer sees slots a MANAGER created without a
 * trainer yet (`GET /without-trainer`) and can self-assign (`POST /{id}/assign`), or drop a
 * future assignment they already hold (`DELETE /{id}/unassign`).
 */
export function TrainerAppointmentsPage() {
  const [mine, setMine] = useState<AppointmentDTO[]>([])
  const [unassigned, setUnassigned] = useState<AppointmentDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function reload() {
    setLoading(true)
    try {
      const [mineRes, unassignedRes] = await Promise.all([
        getMyAppointmentsAsTrainer(),
        getAppointmentsWithoutTrainer(),
      ])
      setMine(mineRes)
      setUnassigned(unassignedRes)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
  }, [])

  async function handleAssign(id: number) {
    setError(null)
    setBusyId(id)
    try {
      await assignSelfToAppointment(id)
      await reload()
    } catch (err) {
      setError(extractErrorMessage(err, 'Dodela nije uspela.'))
    } finally {
      setBusyId(null)
    }
  }

  async function handleUnassign(id: number) {
    setError(null)
    setBusyId(id)
    try {
      await unassignSelfFromAppointment(id)
      await reload()
    } catch (err) {
      setError(extractErrorMessage(err, 'Otkazivanje dodele nije uspelo.'))
    } finally {
      setBusyId(null)
    }
  }

  const upcomingMine = mine.filter(isUpcoming)
  const pastMine = mine.filter(isPast)
  const upcomingUnassigned = unassigned.filter(isUpcoming)

  function itemLabel(a: AppointmentDTO) {
    return (
      <>
        {a.date} · {a.startTime.slice(0, 5)}–{a.endTime.slice(0, 5)} ·{' '}
        <span className="text-slate-400">
          {SESSION_TYPE_LABEL[a.session.type] ?? a.session.type} · {a.clients.length}/
          {a.session.maxParticipants}
        </span>
      </>
    )
  }

  return (
    <div className="p-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-100">Moji termini</h1>
      <p className="mb-6 text-sm text-slate-500">
        Termini na koje si dodeljen, i slobodni termini bez trenera na koje se možeš sam prijaviti.
      </p>

      {error && <p className="mb-4 rounded-lg bg-red-950/60 px-3 py-2 text-sm text-red-300">{error}</p>}

      <div className="mb-6 rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Budući dodeljeni termini</h3>
        {loading ? (
          <p className="text-sm text-slate-500">Učitavanje...</p>
        ) : upcomingMine.length === 0 ? (
          <p className="text-sm text-slate-500">Nema budućih dodeljenih termina.</p>
        ) : (
          <ul className="space-y-1 text-sm">
            {upcomingMine.map((a) => (
              <li
                key={a.id}
                className="flex items-center justify-between rounded-lg bg-slate-950/60 px-3 py-2"
              >
                <span className="text-slate-300">{itemLabel(a)}</span>
                <button
                  onClick={() => handleUnassign(a.id)}
                  disabled={busyId === a.id}
                  className="rounded-lg border border-red-900/50 px-2 py-0.5 text-xs text-red-300 hover:bg-red-950/40 disabled:opacity-60"
                >
                  {busyId === a.id ? '...' : 'Otkaži dodelu'}
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="mb-6 rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Termini bez trenera</h3>
        {loading ? (
          <p className="text-sm text-slate-500">Učitavanje...</p>
        ) : upcomingUnassigned.length === 0 ? (
          <p className="text-sm text-slate-500">Trenutno nema slobodnih termina bez trenera.</p>
        ) : (
          <ul className="space-y-1 text-sm">
            {upcomingUnassigned.map((a) => (
              <li
                key={a.id}
                className="flex items-center justify-between rounded-lg bg-slate-950/60 px-3 py-2"
              >
                <span className="text-slate-300">{itemLabel(a)}</span>
                <button
                  onClick={() => handleAssign(a.id)}
                  disabled={busyId === a.id}
                  className="rounded-lg bg-brand-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-brand-500 disabled:opacity-60"
                >
                  {busyId === a.id ? '...' : 'Prijavi se'}
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Istorija dodeljenih termina</h3>
        {loading ? (
          <p className="text-sm text-slate-500">Učitavanje...</p>
        ) : pastMine.length === 0 ? (
          <p className="text-sm text-slate-500">Još nema odrađenih termina.</p>
        ) : (
          <ul className="space-y-1 text-sm">
            {pastMine.map((a) => (
              <li key={a.id} className="rounded-lg bg-slate-950/60 px-3 py-2 text-slate-400">
                {itemLabel(a)}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
