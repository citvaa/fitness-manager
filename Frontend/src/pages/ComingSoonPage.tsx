import { useAuthStore } from '../auth/authStore'

export function ComingSoonPage() {
  const role = useAuthStore((state)=>state.session?.activeRole)
  return <main className="placeholder-page"><div className="placeholder-orbit">↗</div><p className="eyebrow">{role === 'TRAINER' ? 'Trenerski portal' : 'Klijentski portal'}</p><h1>Praćenje napretka</h1><p>Personalizovani grafikoni, rekordi i pregled napretka stižu u sledećoj fazi.</p></main>
}
