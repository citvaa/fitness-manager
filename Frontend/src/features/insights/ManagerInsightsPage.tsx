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

  // The backend prompt (see AGENTS.md "Upgrade: manager-testing fixes") asks Claude for a short
  // summary paragraph followed by "- "-prefixed recommendation lines - rendered here as an
  // actual <ul>/<li> list instead of dumping everything into one dense text blob. Consecutive
  // bullet lines are grouped into a single list; everything else becomes its own paragraph.
  const lines = insights?.insightText.split(/\n+/).filter((line) => line.trim().length > 0) ?? []
  const blocks: { type: 'paragraph' | 'list'; items: string[] }[] = []
  for (const line of lines) {
    const bulletMatch = /^[-•]\s*(.+)/.exec(line.trim())
    if (bulletMatch) {
      const last = blocks[blocks.length - 1]
      if (last?.type === 'list') {
        last.items.push(bulletMatch[1])
      } else {
        blocks.push({ type: 'list', items: [bulletMatch[1]] })
      }
    } else {
      blocks.push({ type: 'paragraph', items: [line.trim()] })
    }
  }

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
            {blocks.length > 0 ? (
              blocks.map((block, i) =>
                block.type === 'list' ? (
                  <ul key={i} className="list-disc space-y-1.5 pl-5">
                    {block.items.map((item, j) => (
                      <li key={j}>{item}</li>
                    ))}
                  </ul>
                ) : (
                  <p key={i}>{block.items[0]}</p>
                ),
              )
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
