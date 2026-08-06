import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { errorMessage, login } from '../api/client'
import { useAuthStore } from '../auth/authStore'

export function LoginPage() {
  const session = useAuthStore((state) => state.session)
  const setSession = useAuthStore((state) => state.setSession)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  if (session) return <Navigate to="/app" replace />

  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setLoading(true)
    try {
      setSession(await login(email, password))
      navigate((location.state as { from?: string } | null)?.from ?? '/app', { replace: true })
    } catch (err) { setError(errorMessage(err)) } finally { setLoading(false) }
  }

  return <main className="login-page">
    <section className="login-story" aria-label="Dobrodošlica">
      <div className="brand-mark"><span>FM</span> GymOS</div>
      <div className="story-copy">
        <p className="eyebrow">Pametniji ritam teretane</p>
        <h1>Svaki prostor.<br/><em>Jedan pogled.</em></h1>
        <p>Upravljajte salama, pratite energiju uživo i donesite bolje odluke — bez napuštanja kontrolne table.</p>
      </div>
      <div className="pulse-card"><span className="pulse-dot"/><div><strong>Live floor</strong><small>Zauzetost se osvežava u realnom vremenu</small></div></div>
    </section>
    <section className="login-panel">
      <form onSubmit={submit} className="login-form">
        <p className="eyebrow">Dobro došli nazad</p><h2>Prijavite se</h2>
        <p className="muted">Unesite podatke svog Fitness Manager naloga.</p>
        <label>Email ili korisničko ime<input autoFocus required type="text" inputMode="email" autoComplete="username" value={email} onChange={(e)=>setEmail(e.target.value)} placeholder="ime@teretana.rs" /></label>
        <label>Lozinka<input required type="password" value={password} onChange={(e)=>setPassword(e.target.value)} placeholder="••••••••" /></label>
        {error && <div className="form-error" role="alert">{error}</div>}
        <button className="primary-button" disabled={loading}>{loading ? 'Prijavljivanje…' : 'Uđi u GymOS'}<span>→</span></button>
        <small className="secure-note">🔒 Sesija se bezbedno obnavlja u pozadini</small>
      </form>
    </section>
  </main>
}
