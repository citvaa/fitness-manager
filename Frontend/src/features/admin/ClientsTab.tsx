import { type FormEvent, useEffect, useState } from 'react'
import { createClient, deleteClient, getClients } from './api'
import type { ClientDTO } from './types'
import { LoadingIndicator } from '../../components/LoadingIndicator'
import { useConfirm } from '../../components/ConfirmDialog'

/**
 * Delete added as part of the "exactly one operational role per user" round -
 * ClientServiceImpl.delete() (mirroring TrainerServiceImpl.delete()) removes the Client domain
 * row/its owned data and the CLIENT role, leaving the User account itself intact. See AGENTS.md
 * "Upgrade: operational-role cardinality decisions".
 */
export function ClientsTab() {
  const [clients, setClients] = useState<ClientDTO[]>([])
  const [loading, setLoading] = useState(true)

  const [email, setEmail] = useState('')
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const confirm = useConfirm()

  async function reload() {
    setLoading(true)
    try {
      setClients(await getClients())
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
  }, [])

  async function handleCreate(e: FormEvent) {
    e.preventDefault()
    setCreating(true)
    setCreateError(null)
    try {
      await createClient(email)
      setEmail('')
      await reload()
    } catch {
      setCreateError('Kreiranje klijenta nije uspelo - proveri da email već ne postoji.')
    } finally {
      setCreating(false)
    }
  }

  async function handleDelete(id: number) {
    if (!(await confirm('Obrisati ovog klijenta? Ovo briše i njegove uplate/termine/napredak.'))) return
    setDeleteError(null)
    try {
      await deleteClient(id)
      await reload()
    } catch {
      setDeleteError('Brisanje klijenta nije uspelo.')
    }
  }

  return (
    <div className="space-y-6">
      <form
        onSubmit={handleCreate}
        className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4"
      >
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Novi klijent</h3>
        <div className="flex flex-wrap items-end gap-3">
          <label className="block text-xs text-slate-400">
            Email
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 w-64 rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
          <button
            type="submit"
            disabled={creating}
            className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-500 disabled:opacity-60"
          >
            {creating ? 'Kreiranje...' : 'Kreiraj klijenta'}
          </button>
        </div>
        {createError && <p className="mt-3 text-xs text-red-400">{createError}</p>}
      </form>

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Klijenti ({clients.length})</h3>
        {deleteError && <p className="mb-3 text-xs text-red-400">{deleteError}</p>}
        {loading ? (
          <LoadingIndicator className="text-sm text-slate-500" />
        ) : clients.length === 0 ? (
          <p className="text-sm text-slate-500">Nema klijenata.</p>
        ) : (
          <ul className="space-y-1">
            {clients.map((c) => (
              <li
                key={c.id}
                className="flex items-center justify-between rounded-lg bg-slate-950/60 px-3 py-2 text-sm text-slate-300"
              >
                <span>{c.user.email}</span>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-slate-500">
                    {c.user.isActivated ? 'Aktivan' : 'Neaktiviran'}
                  </span>
                  <button
                    onClick={() => handleDelete(c.id)}
                    className="rounded-lg border border-red-900/50 px-2 py-0.5 text-xs text-red-300 hover:bg-red-950/40"
                  >
                    Obriši
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
