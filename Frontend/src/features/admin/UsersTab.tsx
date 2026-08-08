import { type FormEvent, useEffect, useState } from 'react'
import type { Role } from '../../auth/types'
import { addUserRole, createUser, deleteUser, getUsers, removeUserRole, updateUser } from './api'
import { ActivationLinkBanner } from './ActivationLinkBanner'
import type { UserDTO } from './types'

const ROLE_LABEL: Record<Role, string> = { MANAGER: 'Menadžer', TRAINER: 'Trener', CLIENT: 'Klijent' }
const PAGE_SIZE = 10

export function UsersTab() {
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [search, setSearch] = useState('')
  const [users, setUsers] = useState<UserDTO[]>([])
  const [loading, setLoading] = useState(true)

  const [newEmail, setNewEmail] = useState('')
  const [newIsManager, setNewIsManager] = useState(false)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [lastCreatedKey, setLastCreatedKey] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [editEmail, setEditEmail] = useState('')

  async function reload() {
    setLoading(true)
    try {
      const result = await getUsers(page, PAGE_SIZE, search)
      setUsers(result.content)
      setTotalPages(result.totalPages)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  function handleSearch(e: FormEvent) {
    e.preventDefault()
    setPage(0)
    void reload()
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault()
    setCreating(true)
    setCreateError(null)
    setLastCreatedKey(null)
    try {
      const created = await createUser(newEmail)
      if (newIsManager) {
        await addUserRole(created.id, 'MANAGER')
      }
      setNewEmail('')
      setNewIsManager(false)
      setLastCreatedKey(created.registrationKey)
      await reload()
    } catch {
      setCreateError('Kreiranje naloga nije uspelo - proveri da email već ne postoji.')
    } finally {
      setCreating(false)
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Obrisati ovaj nalog?')) return
    await deleteUser(id)
    await reload()
  }

  function startEdit(user: UserDTO) {
    setEditingId(user.id)
    setEditEmail(user.email)
  }

  async function saveEdit(id: number) {
    await updateUser(id, editEmail)
    setEditingId(null)
    await reload()
  }

  async function toggleManagerRole(user: UserDTO) {
    if (user.roles.includes('MANAGER')) {
      await removeUserRole(user.id, 'MANAGER')
    } else {
      await addUserRole(user.id, 'MANAGER')
    }
    await reload()
  }

  return (
    <div className="space-y-6">
      <form
        onSubmit={handleCreate}
        className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4"
      >
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Novi nalog</h3>
        <p className="mb-3 text-xs text-slate-500">
          Kreira "prazan" nalog (samo email) - koristi ovo za menadžerske naloge. Za trenere i
          klijente koristi tabove "Treneri"/"Klijenti" ispod, koji uz nalog odmah kreiraju i
          odgovarajući domenski profil.
        </p>
        <div className="flex flex-wrap items-end gap-3">
          <label className="block text-xs text-slate-400">
            Email
            <input
              type="email"
              required
              value={newEmail}
              onChange={(e) => setNewEmail(e.target.value)}
              className="mt-1 w-64 rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
          <label className="flex items-center gap-2 pb-1.5 text-xs text-slate-400">
            <input
              type="checkbox"
              checked={newIsManager}
              onChange={(e) => setNewIsManager(e.target.checked)}
            />
            Dodeli MANAGER rolu odmah
          </label>
          <button
            type="submit"
            disabled={creating}
            className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-500 disabled:opacity-60"
          >
            {creating ? 'Kreiranje...' : 'Kreiraj nalog'}
          </button>
        </div>
        {createError && <p className="mt-3 text-xs text-red-400">{createError}</p>}
        {lastCreatedKey && (
          <div className="mt-3">
            <ActivationLinkBanner registrationKey={lastCreatedKey} />
          </div>
        )}
      </form>

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <form onSubmit={handleSearch} className="mb-4 flex gap-2">
          <input
            type="text"
            placeholder="Pretraga po emailu..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-64 rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none focus:border-brand-500"
          />
          <button
            type="submit"
            className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-300 hover:bg-slate-800"
          >
            Pretraži
          </button>
        </form>

        {loading ? (
          <p className="text-sm text-slate-500">Učitavanje...</p>
        ) : users.length === 0 ? (
          <p className="text-sm text-slate-500">Nema korisnika.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-800 text-xs uppercase tracking-wide text-slate-500">
                  <th className="py-2 pr-4">Email</th>
                  <th className="py-2 pr-4">Role</th>
                  <th className="py-2 pr-4">Aktivan</th>
                  <th className="py-2 pr-4">Akcije</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id} className="border-b border-slate-800/60">
                    <td className="py-2 pr-4">
                      {editingId === u.id ? (
                        <input
                          value={editEmail}
                          onChange={(e) => setEditEmail(e.target.value)}
                          className="w-56 rounded-lg border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100 outline-none focus:border-brand-500"
                        />
                      ) : (
                        u.email
                      )}
                    </td>
                    <td className="py-2 pr-4">
                      <div className="flex flex-wrap gap-1">
                        {u.roles.map((r) => (
                          <span
                            key={r}
                            className="rounded-full bg-slate-800 px-2 py-0.5 text-xs text-slate-300"
                          >
                            {ROLE_LABEL[r]}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="py-2 pr-4 text-slate-400">{u.isActivated ? 'Da' : 'Ne'}</td>
                    <td className="py-2 pr-4">
                      <div className="flex flex-wrap gap-2">
                        {editingId === u.id ? (
                          <>
                            <button
                              onClick={() => saveEdit(u.id)}
                              className="rounded-lg bg-brand-600 px-2 py-1 text-xs text-white hover:bg-brand-500"
                            >
                              Sačuvaj
                            </button>
                            <button
                              onClick={() => setEditingId(null)}
                              className="rounded-lg border border-slate-700 px-2 py-1 text-xs text-slate-300 hover:bg-slate-800"
                            >
                              Otkaži
                            </button>
                          </>
                        ) : (
                          <button
                            onClick={() => startEdit(u)}
                            className="rounded-lg border border-slate-700 px-2 py-1 text-xs text-slate-300 hover:bg-slate-800"
                          >
                            Izmeni email
                          </button>
                        )}
                        <button
                          onClick={() => toggleManagerRole(u)}
                          className="rounded-lg border border-slate-700 px-2 py-1 text-xs text-slate-300 hover:bg-slate-800"
                          title="TRAINER/CLIENT role se dodeljuju kroz tabove Treneri/Klijenti, jer tamo se uz rolu kreira i domenski profil"
                        >
                          {u.roles.includes('MANAGER') ? 'Oduzmi MANAGER' : 'Dodaj MANAGER'}
                        </button>
                        <button
                          onClick={() => handleDelete(u.id)}
                          className="rounded-lg border border-red-900/50 px-2 py-1 text-xs text-red-300 hover:bg-red-950/40"
                        >
                          Obriši
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {totalPages > 1 && (
          <div className="mt-4 flex items-center gap-2 text-sm text-slate-400">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              className="rounded-lg border border-slate-700 px-3 py-1 disabled:opacity-40"
            >
              ‹
            </button>
            <span>
              Strana {page + 1} / {totalPages}
            </span>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="rounded-lg border border-slate-700 px-3 py-1 disabled:opacity-40"
            >
              ›
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
