import { type FormEvent, useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import type { Role } from '../../auth/types'
import { deleteUser, getUsers, updateUser } from './api'
import type { UserDTO } from './types'
import { LoadingIndicator } from '../../components/LoadingIndicator'
import { useConfirm } from '../../components/ConfirmDialog'

/** See the same helper in features/schedule/TrainerSchedulePage.tsx - surfaces
 * GlobalExceptionHandler's real validation/access-denied message instead of failing silently. */
function extractErrorMessage(err: unknown, fallback: string): string {
  if (isAxiosError(err) && typeof err.response?.data?.message === 'string') {
    return err.response.data.message
  }
  return fallback
}

const ROLE_LABEL: Record<Role, string> = {
  MANAGER: 'Menadžer',
  TRAINER: 'Trener',
  CLIENT: 'Klijent',
  ADMIN: 'Admin',
}
const PAGE_SIZE = 10

/**
 * The "Novi nalog" create form used to live here, with a "Dodeli MANAGER rolu odmah" checkbox -
 * moved to its own "Menadžeri" tab (ManagersTab) that always creates a MANAGER account, the same
 * way "Treneri"/"Klijenti" each default their own role. This tab is now purely the full
 * cross-role account list (search/edit email/delete) - see AGENTS.md ("Upgrade: manager-testing
 * fixes"). There is deliberately no MANAGER-toggle here anymore: since every real account already
 * holds exactly one operational role (see "one operational role per user" in AGENTS.md), converting
 * an existing user into a MANAGER post-hoc is not a representable operation - a MANAGER account is
 * only ever created via the "Menadžeri" tab's own create form.
 */
export function UsersTab() {
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [search, setSearch] = useState('')
  const [users, setUsers] = useState<UserDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [actionError, setActionError] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [editEmail, setEditEmail] = useState('')
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [savingEdit, setSavingEdit] = useState(false)
  const confirm = useConfirm()

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

  async function handleDelete(id: number) {
    if (!(await confirm('Obrisati ovaj nalog?'))) return
    setActionError(null)
    setDeletingId(id)
    try {
      await deleteUser(id)
      await reload()
    } catch (err) {
      setActionError(
        extractErrorMessage(err, 'Brisanje naloga nije uspelo - nalog možda ima povezane podatke.'),
      )
    } finally {
      setDeletingId(null)
    }
  }

  function startEdit(user: UserDTO) {
    setEditingId(user.id)
    setEditEmail(user.email)
  }

  async function saveEdit(id: number) {
    setActionError(null)
    setSavingEdit(true)
    try {
      await updateUser(id, editEmail)
      setEditingId(null)
      await reload()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Izmena naloga nije uspela.'))
    } finally {
      setSavingEdit(false)
    }
  }

  return (
    <div className="space-y-6">
      {actionError && (
        <p className="rounded-lg border border-red-900/50 bg-red-950/30 px-3 py-2 text-xs text-red-400">
          {actionError}
        </p>
      )}

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
          <LoadingIndicator className="text-sm text-slate-500" />
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
                              disabled={savingEdit}
                              className="rounded-lg bg-brand-600 px-2 py-1 text-xs text-white hover:bg-brand-500 disabled:opacity-60"
                            >
                              {savingEdit ? 'Čuvanje...' : 'Sačuvaj'}
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
                          onClick={() => handleDelete(u.id)}
                          disabled={deletingId === u.id}
                          className="rounded-lg border border-red-900/50 px-2 py-1 text-xs text-red-300 hover:bg-red-950/40 disabled:opacity-60"
                        >
                          {deletingId === u.id ? 'Brišem...' : 'Obriši'}
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
