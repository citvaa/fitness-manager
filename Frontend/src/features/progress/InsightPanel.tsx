import { useState } from 'react'
import type { ClientProgressInsightDTO } from './types'

function formatGeneratedAt(iso: string) {
  return new Date(iso).toLocaleString('sr-RS', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function InsightPanel({
  insight,
  loading,
  onRefresh,
}: {
  insight: ClientProgressInsightDTO | null
  loading: boolean
  onRefresh: () => Promise<void>
}) {
  const [refreshing, setRefreshing] = useState(false)

  async function handleRefresh() {
    setRefreshing(true)
    try {
      await onRefresh()
    } finally {
      setRefreshing(false)
    }
  }

  const paragraphs = insight?.narrative.split(/\n+/).filter((p) => p.trim().length > 0) ?? []

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-slate-300">🤖 AI rezime napretka</h3>
        <button
          onClick={handleRefresh}
          disabled={refreshing || loading}
          className="rounded-lg border border-slate-700 px-3 py-1 text-xs text-slate-300 hover:bg-slate-800 disabled:opacity-60"
        >
          {refreshing ? 'Osvežavanje...' : '🔄 Osveži'}
        </button>
      </div>

      {loading ? (
        <p className="text-sm text-slate-500">Učitavanje...</p>
      ) : insight ? (
        <>
          <div className="space-y-2 text-sm leading-relaxed text-slate-200">
            {paragraphs.map((p, i) => (
              <p key={i}>{p}</p>
            ))}
          </div>
          <p className="mt-4 border-t border-slate-800 pt-2 text-xs text-slate-500">
            Generisano: {formatGeneratedAt(insight.generatedAt)}
          </p>
        </>
      ) : (
        <p className="text-sm text-slate-500">Nema dovoljno podataka za rezime.</p>
      )}
    </div>
  )
}
