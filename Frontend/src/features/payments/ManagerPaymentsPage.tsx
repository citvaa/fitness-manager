import { type FormEvent, useEffect, useMemo, useState } from 'react'
import { DateInput } from '../../components/DateInput'
import { SearchableSelect } from '../../components/SearchableSelect'
import { createPayment, getAllPayments, getClientsForPicker, getPaymentStatus, getSessions } from './api'
import { PaymentStatusSummary } from './PaymentStatusSummary'
import type { ClientSummaryDTO, PaymentDTO, SessionDTO, SessionTypePaymentStatusDTO } from './types'
import { LoadingIndicator } from '../../components/LoadingIndicator'

const SESSION_TYPE_LABEL: Record<string, string> = { INDIVIDUAL: 'Individualni', GROUP: 'Grupni' }

export function ManagerPaymentsPage() {
  const [payments, setPayments] = useState<PaymentDTO[]>([])
  const [clients, setClients] = useState<ClientSummaryDTO[]>([])
  const [sessions, setSessions] = useState<SessionDTO[]>([])
  const [loading, setLoading] = useState(true)

  const [filterClientId, setFilterClientId] = useState<number | ''>('')
  const [status, setStatus] = useState<SessionTypePaymentStatusDTO[] | null>(null)
  const [statusLoading, setStatusLoading] = useState(false)

  const [clientId, setClientId] = useState<number | ''>('')
  const [sessionId, setSessionId] = useState<number | ''>('')
  const [paidAppointments, setPaidAppointments] = useState(1)
  const [paymentDate, setPaymentDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  const clientOptions = useMemo(() => clients.map((c) => ({ value: c.id, label: c.email })), [clients])

  async function reload(clientFilter?: number) {
    setLoading(true)
    try {
      setPayments(await getAllPayments(clientFilter))
    } finally {
      setLoading(false)
    }
  }

  async function reloadStatus(id: number) {
    setStatusLoading(true)
    try {
      setStatus(await getPaymentStatus(id))
    } finally {
      setStatusLoading(false)
    }
  }

  useEffect(() => {
    void reload()
    getClientsForPicker().then(setClients)
    getSessions().then(setSessions)
  }, [])

  function handleFilterChange(id: number | '') {
    setFilterClientId(id)
    void reload(id || undefined)
    if (id) {
      void reloadStatus(id)
    } else {
      setStatus(null)
    }
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault()
    if (!clientId || !sessionId) return
    setCreating(true)
    setCreateError(null)
    try {
      await createPayment({
        clientId,
        sessionId,
        paidAppointments,
        paymentDate,
      })
      setPaidAppointments(1)
      await reload(filterClientId || undefined)
      // Keep the debt summary in sync if a payment was just recorded for the currently-filtered
      // client - a payment for a different client doesn't affect what's shown.
      if (filterClientId && filterClientId === clientId) {
        await reloadStatus(filterClientId)
      }
    } catch {
      setCreateError('Unos uplate nije uspeo.')
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="p-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-100">Plaćanja</h1>
      <p className="mb-6 text-sm text-slate-500">Istorija uplata i unos nove uplate za klijenta.</p>

      <form
        onSubmit={handleCreate}
        className="mb-6 rounded-2xl border border-slate-800 bg-slate-900/40 p-4"
      >
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Nova uplata</h3>
        <div className="grid gap-3 md:grid-cols-4">
          <label className="block text-xs text-slate-400">
            Klijent
            <SearchableSelect
              required
              options={clientOptions}
              value={clientId}
              onChange={setClientId}
              placeholder="Pretraži klijenta..."
            />
          </label>
          <label className="block text-xs text-slate-400">
            Tip sesije
            <select
              required
              value={sessionId}
              onChange={(e) => setSessionId(e.target.value ? Number(e.target.value) : '')}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            >
              <option value="">Izaberi...</option>
              {sessions.map((s) => (
                <option key={s.id} value={s.id}>
                  {SESSION_TYPE_LABEL[s.type] ?? s.type} (max {s.maxParticipants})
                </option>
              ))}
            </select>
          </label>
          <label className="block text-xs text-slate-400">
            Broj plaćenih termina
            <input
              type="number"
              min={1}
              required
              value={paidAppointments}
              onChange={(e) => setPaidAppointments(Number(e.target.value))}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
          <label className="block text-xs text-slate-400">
            Datum uplate
            <DateInput
              required
              value={paymentDate}
              onChange={setPaymentDate}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
        </div>
        <button
          type="submit"
          disabled={creating}
          className="mt-3 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-500 disabled:opacity-60"
        >
          {creating ? 'Čuvanje...' : 'Zabeleži uplatu'}
        </button>
        {createError && <p className="mt-3 text-xs text-red-400">{createError}</p>}
      </form>

      {filterClientId && (
        <div className="mb-6">
          {statusLoading || !status ? (
            <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
              <LoadingIndicator className="text-sm text-slate-500" />
            </div>
          ) : (
            <PaymentStatusSummary status={status} />
          )}
        </div>
      )}

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-slate-300">Istorija uplata</h3>
          <div className="w-64">
            <SearchableSelect
              options={clientOptions}
              value={filterClientId}
              onChange={handleFilterChange}
              placeholder="Svi klijenti (pretraži)..."
            />
          </div>
        </div>

        {loading ? (
          <LoadingIndicator className="text-sm text-slate-500" />
        ) : payments.length === 0 ? (
          <p className="text-sm text-slate-500">Nema uplata.</p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-800 text-xs uppercase tracking-wide text-slate-500">
                <th className="py-2 pr-4">Datum</th>
                <th className="py-2 pr-4">Klijent</th>
                <th className="py-2 pr-4">Tip sesije</th>
                <th className="py-2 pr-4">Plaćeni termini</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((p) => (
                <tr key={p.id} className="border-b border-slate-800/60">
                  <td className="py-2 pr-4 text-slate-300">{p.paymentDate}</td>
                  <td className="py-2 pr-4 text-slate-300">{p.client.email}</td>
                  <td className="py-2 pr-4 text-slate-400">
                    {SESSION_TYPE_LABEL[p.session.type] ?? p.session.type}
                  </td>
                  <td className="py-2 pr-4 text-slate-400">{p.paidAppointments}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
