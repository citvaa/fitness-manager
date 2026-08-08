import { type FormEvent, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { completeRegistration } from '../auth/api'

/**
 * Public "finish registration" screen for the invite-based flow: a MANAGER creates a blank
 * account (email only), the backend generates a registrationKey, and normally that key reaches
 * the user via an activation email. Since MAIL_USERNAME/MAIL_PASSWORD aren't configured with a
 * real Gmail account in this environment, the admin screen instead surfaces the full activation
 * link on-screen (see AdminPage) - this page is what that link points to. See AGENTS.md
 * "Upgrade: Faza 6 decisions" for the full rationale.
 */
export function CompleteRegistrationPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  const [registrationKey, setRegistrationKey] = useState(searchParams.get('registration_key') ?? '')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)

    if (password !== confirmPassword) {
      setError('Lozinke se ne poklapaju.')
      return
    }
    if (!registrationKey) {
      setError('Nedostaje registracioni ključ.')
      return
    }

    setSubmitting(true)
    try {
      await completeRegistration(registrationKey, password)
      setDone(true)
    } catch {
      setError('Aktivacija nije uspela - ključ je nevažeći, istekao je, ili je već iskorišćen.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-950 px-4">
      <div className="w-full max-w-sm rounded-2xl border border-slate-800 bg-slate-900/60 p-8 shadow-2xl shadow-black/40 backdrop-blur">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-brand-500/20 text-2xl">
            🏋️
          </div>
          <h1 className="text-xl font-semibold text-slate-100">Dovrši registraciju</h1>
          <p className="mt-1 text-sm text-slate-400">Postavi lozinku da aktiviraš svoj nalog</p>
        </div>

        {done ? (
          <div className="space-y-4 text-center">
            <p className="rounded-lg bg-emerald-950/60 px-3 py-2 text-sm text-emerald-300">
              Nalog je aktiviran. Sada se možeš prijaviti.
            </p>
            <button
              onClick={() => navigate('/login', { replace: true })}
              className="w-full rounded-lg bg-brand-600 px-3 py-2 font-medium text-white transition hover:bg-brand-500"
            >
              Idi na prijavu
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-300">
                Registracioni ključ
              </label>
              <input
                type="text"
                required
                value={registrationKey}
                onChange={(e) => setRegistrationKey(e.target.value)}
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500"
                placeholder="iz aktivacionog linka"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-300">Nova lozinka</label>
              <input
                type="password"
                required
                autoFocus={!!registrationKey}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500"
                placeholder="••••••••"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-300">Potvrdi lozinku</label>
              <input
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500"
                placeholder="••••••••"
              />
            </div>

            {error && (
              <p className="rounded-lg bg-red-950/60 px-3 py-2 text-sm text-red-300">{error}</p>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="w-full rounded-lg bg-brand-600 px-3 py-2 font-medium text-white transition hover:bg-brand-500 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {submitting ? 'Aktivacija...' : 'Aktiviraj nalog'}
            </button>

            <p className="text-center text-xs text-slate-500">
              Već imaš nalog?{' '}
              <Link to="/login" className="text-brand-400 hover:underline">
                Prijavi se
              </Link>
            </p>
          </form>
        )}
      </div>
    </div>
  )
}
