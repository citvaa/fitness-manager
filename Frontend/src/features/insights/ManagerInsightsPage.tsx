import { useEffect, useState } from 'react'
import { getManagerInsights, refreshManagerInsights } from './api'
import type { ManagerInsightsDTO } from './types'

function formatGeneratedAt(iso: string) {
  return new Date(iso).toLocaleString('sr-RS', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function ManagerInsightsPage() {
  const [insights, setInsights] = useState<ManagerInsightsDTO | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setInsights(await getManagerInsights())
    } catch {
      setError('Uvid trenutno nije dostupan.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function handleRefresh() {
    setRefreshing(true)
    setError(null)
    try {
      setInsights(await refreshManagerInsights())
    } catch {
      setError('Regenerisanje nije uspelo. Pokušaj ponovo.')
    } finally {
      setRefreshing(false)
    }
  }

  if (loading) {
    return <div className="p-8 text-slate-400">Učitavanje...</div>
  }

  const paragraphs = insights?.insightText.split(/\n+/).filter((p) => p.trim().length > 0) ?? []

  return (
    <div className="mx-auto max-w-3xl p-6">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-slate-100">AI uvid za menadžera</h1>
          <p className="text-sm text-slate-500">
            Automatski generisan pregled na osnovu poslednjih {insights?.periodDays ?? 30} dana
            podataka teretane.
          </p>
        </div>
        <button
          onClick={handleRefresh}
          disabled={refreshing}
          className="shrink-0 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-500 disabled:opacity-60"
        >
          {refreshing ? 'Generisanje...' : '🔄 Regeneriši'}
        </button>
      </div>

      {error && (
        <div className="mb-4 rounded-lg border border-red-900/50 bg-red-950/40 px-4 py-2 text-sm text-red-300">
          {error}
        </div>
      )}

      {insights && (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-6">
          <div className="space-y-3 text-sm leading-relaxed text-slate-200">
            {paragraphs.length > 0 ? (
              paragraphs.map((p, i) => <p key={i}>{p}</p>)
            ) : (
              <p className="text-slate-500">Nema dostupnog teksta.</p>
            )}
          </div>
          <div className="mt-6 border-t border-slate-800 pt-3 text-xs text-slate-500">
            Generisano: {formatGeneratedAt(insights.generatedAt)}
          </div>
        </div>
      )}
    </div>
  )
}
