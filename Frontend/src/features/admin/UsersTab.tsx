import { type FormEvent, useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import type { Role } from '../../auth/types'
import { useAuthStore } from '../../auth/store'
import { addUserRole, deleteUser, getUsers, removeUserRole, updateUser } from './api'
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
 * cross-role account list (search/edit email/toggle MANAGER/delete) - see AGENTS.md
 * ("Upgrade: manager-testing fixes").
 */
export function UsersTab() {
  const currentUserId = useAuthStore((s) => s.user?.id)
  // Only an ADMIN may grant/revoke MANAGER - backend-enforced (UserServiceImpl
  // addRole/removeRole -> 403 for non-ADMIN callers); this just hides the now-unusable button
  // for everyone else instead of leaving it clickable-but-always-403.
  const isAdmin = useAuthStore((s) => s.user?.roles.includes('ADMIN') ?? false)

  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [search, setSearch] = useState('')
  const [users, setUsers] = useState<UserDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [actionError, setActionError] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [editEmail, setEditEmail] = useState('')
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
    try {
      await deleteUser(id)
      await reload()
    } catch (err) {
      setActionError(
        extractErrorMessage(err, 'Brisanje naloga nije uspelo - nalog možda ima povezane podatke.'),
      )
    }
  }

  function startEdit(user: UserDTO) {
    setEditingId(user.id)
    setEditEmail(user.email)
  }

  async function saveEdit(id: number) {
    setActionError(null)
    try {
      await updateUser(id, editEmail)
      setEditingId(null)
      await reload()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Izmena naloga nije uspela.'))
    }
  }

  async function toggleManagerRole(user: UserDTO) {
    setActionError(null)
    try {
      if (user.roles.includes('MANAGER')) {
        await removeUserRole(user.id, 'MANAGER')
      } else {
        await addUserRole(user.id, 'MANAGER')
      }
      await reload()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Izmena role nije uspela.'))
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
                        {isAdmin && (
                          <button
                            onClick={() => toggleManagerRole(u)}
                            disabled={u.roles.includes('MANAGER') && u.id === currentUserId}
                            className="rounded-lg border border-slate-700 px-2 py-1 text-xs text-slate-300 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
                            title={
                              u.roles.includes('MANAGER') && u.id === currentUserId
                                ? 'Ne možete sami sebi oduzeti MANAGER rolu'
                                : 'TRAINER/CLIENT role se dodeljuju kroz tabove Treneri/Klijenti, jer tamo se uz rolu kreira i domenski profil'
                            }
                          >
                            {u.roles.includes('MANAGER') ? 'Oduzmi MANAGER' : 'Dodaj MANAGER'}
                          </button>
                        )}
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
