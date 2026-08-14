import { useState } from 'react'
import type { ClientProgressInsightDTO } from './types'

/**
 * Parses the "short intro + bullets" shape the backend prompt now asks for (see AGENTS.md
 * "Upgrade: progress-insight readability decisions") - intro is every line before the first
 * "- "-prefixed bullet line, bullets are every line starting with "- " (prefix stripped). Falls
 * back gracefully to treating the whole thing as intro-only if the model didn't produce any
 * bullet lines, rather than failing to render anything.
 */
function parseNarrative(narrative: string): { intro: string[]; bullets: string[] } {
  const lines = narrative.split('\n').map((l) => l.trim())
  const introLines: string[] = []
  const bullets: string[] = []
  for (const line of lines) {
    if (line.startsWith('- ')) {
      bullets.push(line.slice(2).trim())
    } else if (line.length > 0 && bullets.length === 0) {
      introLines.push(line)
    }
  }
  return { intro: introLines, bullets }
}

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

  const { intro, bullets } = insight ? parseNarrative(insight.narrative) : { intro: [], bullets: [] }

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
            {intro.map((p, i) => (
              <p key={i}>{p}</p>
            ))}
            {bullets.length > 0 && (
              <ul className="list-disc space-y-1 pl-4">
                {bullets.map((b, i) => (
                  <li key={i}>{b}</li>
                ))}
              </ul>
            )}
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
