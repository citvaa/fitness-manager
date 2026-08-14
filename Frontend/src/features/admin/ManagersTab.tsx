import { type FormEvent, useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import { useAuthStore } from '../../auth/store'
import { addUserRole, createUser, getUsers } from './api'
import type { UserDTO } from './types'
import { LoadingIndicator } from '../../components/LoadingIndicator'

/** See the same helper in features/schedule/TrainerSchedulePage.tsx - surfaces
 * GlobalExceptionHandler's real validation/access-denied message instead of failing silently. */
function extractErrorMessage(err: unknown, fallback: string): string {
  if (isAxiosError(err) && typeof err.response?.data?.message === 'string') {
    return err.response.data.message
  }
  return fallback
}

/**
 * Split out of UsersTab's old "Novi nalog" form (see AGENTS.md "Upgrade: manager-testing
 * fixes") - mirrors the "Treneri"/"Klijenti" tabs' pattern of one dedicated create form per
 * role, each defaulting its own role rather than exposing a "create with no role at all" option
 * or a "assign role now?" checkbox. A generic account with no role is never a valid outcome of
 * this form. The full cross-role list (search/edit/delete/toggle role) stays in "Korisnici"
 * (UsersTab) - this tab only creates.
 */
export function ManagersTab() {
  // Only an ADMIN may grant MANAGER - backend-enforced (UserServiceImpl.addRole -> 403 for
  // non-ADMIN callers); this hides the now-unusable create form for everyone else.
  const isAdmin = useAuthStore((s) => s.user?.roles.includes('ADMIN') ?? false)
  const [managers, setManagers] = useState<UserDTO[]>([])
  const [loading, setLoading] = useState(true)

  const [email, setEmail] = useState('')
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  async function reload() {
    setLoading(true)
    try {
      // No dedicated "list managers" endpoint - the generic /api/user list, filtered client-side,
      // matches this tab's narrow scope (create + see the managers that resulted) without adding
      // a new backend query just for this display.
      const result = await getUsers(0, 100, undefined)
      setManagers(result.content.filter((u) => u.roles.includes('MANAGER')))
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
      const created = await createUser(email)
      await addUserRole(created.id, 'MANAGER')
      setEmail('')
      await reload()
    } catch (err) {
      setCreateError(
        extractErrorMessage(err, 'Kreiranje menadžera nije uspelo - proveri da email već ne postoji.'),
      )
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="space-y-6">
      {isAdmin ? (
        <form
          onSubmit={handleCreate}
          className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4"
        >
          <h3 className="mb-3 text-sm font-semibold text-slate-300">Novi menadžer</h3>
          <p className="mb-3 text-xs text-slate-500">
            Kreira nalog i odmah dodeljuje MANAGER rolu - aktivacioni link stiže na uneti email.
          </p>
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
              {creating ? 'Kreiranje...' : 'Kreiraj menadžera'}
            </button>
          </div>
          {createError && <p className="mt-3 text-xs text-red-400">{createError}</p>}
        </form>
      ) : (
        <p className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4 text-sm text-slate-500">
          Samo ADMIN korisnik može dodeliti MANAGER rolu i kreirati nove menadžere.
        </p>
      )}

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Menadžeri ({managers.length})</h3>
        {loading ? (
          <LoadingIndicator className="text-sm text-slate-500" />
        ) : managers.length === 0 ? (
          <p className="text-sm text-slate-500">Nema menadžera.</p>
        ) : (
          <ul className="space-y-1">
            {managers.map((m) => (
              <li
                key={m.id}
                className="flex items-center justify-between rounded-lg bg-slate-950/60 px-3 py-2 text-sm text-slate-300"
              >
                <span>{m.email}</span>
                <span className="text-xs text-slate-500">{m.isActivated ? 'Aktivan' : 'Neaktiviran'}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
