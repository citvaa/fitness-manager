import { useEffect, useState } from 'react'
import { getManagerInsights, refreshManagerInsights } from './api'
import type { ManagerInsightsDTO } from './types'
import { RatingBadge, RoomOccupancyChart, SessionTypeChart, StatTile } from './InsightCharts'

function formatGeneratedAt(iso: string) {
  return new Date(iso).toLocaleString('sr-RS', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function SectionCard({
  title,
  subtitle,
  children,
}: {
  title: string
  subtitle?: string
  children: React.ReactNode
}) {
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-5">
      <h2 className="text-sm font-semibold text-slate-200">{title}</h2>
      {subtitle && <p className="mb-3 text-xs text-slate-500">{subtitle}</p>}
      <div className={subtitle ? '' : 'mt-3'}>{children}</div>
    </div>
  )
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

  return (
    <div className="mx-auto max-w-5xl p-6">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
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
        <div className="space-y-4">
          <SectionCard title="Rezime">
            <p className="text-sm leading-relaxed text-slate-200">
              {insights.summary || 'Nema dostupnog rezimea.'}
            </p>
            {insights.recommendations.length > 0 && (
              <ul className="mt-4 space-y-1.5 border-t border-slate-800 pt-4 text-sm text-slate-300">
                {insights.recommendations.map((rec, i) => (
                  <li key={i} className="flex gap-2">
                    <span className="text-brand-400">→</span>
                    <span>{rec}</span>
                  </li>
                ))}
              </ul>
            )}
          </SectionCard>

          <div className="grid gap-4 md:grid-cols-3">
            <StatTile label="Različiti klijenti" value={insights.attendance.distinctClients} />
            <StatTile label="Ukupno check-in-a" value={insights.attendance.totalCheckIns} />
            <StatTile
              label="Prosečno trajanje"
              value={insights.attendance.avgCheckInDurationMinutes}
              suffix="min"
            />
          </div>

          <SectionCard title="Posećenost — AI ocena" subtitle="Ocena ukupne posećenosti teretane u periodu.">
            <div className="flex items-start gap-3">
              <RatingBadge rating={insights.attendance.rating} />
              <p className="text-sm text-slate-300">{insights.attendance.comment}</p>
            </div>
          </SectionCard>

          <SectionCard
            title="Popunjenost po sali"
            subtitle="Broj check-in-a po sali u periodu, obojeno prema AI oceni te sale."
          >
            {insights.roomOccupancy.length > 0 ? (
              <>
                <RoomOccupancyChart rooms={insights.roomOccupancy} />
                <ul className="mt-4 space-y-2 border-t border-slate-800 pt-4">
                  {insights.roomOccupancy.map((room) => (
                    <li key={room.roomName} className="flex items-start gap-2 text-sm">
                      <RatingBadge rating={room.rating} />
                      <span className="font-medium text-slate-200">{room.roomName}:</span>
                      <span className="text-slate-400">{room.comment}</span>
                    </li>
                  ))}
                </ul>
              </>
            ) : (
              <p className="text-sm text-slate-500">Nema sala.</p>
            )}
          </SectionCard>

          <SectionCard
            title="Individualne vs. grupne sesije"
            subtitle="Udeo plaćenih termina po tipu sesije, obojeno prema AI oceni tog odnosa."
          >
            {insights.sessionTypeBreakdown.length > 0 ? (
              <>
                <SessionTypeChart sessions={insights.sessionTypeBreakdown} />
                <ul className="mt-4 space-y-2 border-t border-slate-800 pt-4">
                  {insights.sessionTypeBreakdown.map((s) => (
                    <li key={s.sessionType} className="flex items-start gap-2 text-sm">
                      <RatingBadge rating={s.rating} />
                      <span className="font-medium text-slate-200">{s.sessionType}:</span>
                      <span className="text-slate-400">{s.comment}</span>
                    </li>
                  ))}
                </ul>
              </>
            ) : (
              <p className="text-sm text-slate-500">Nema plaćenih termina u periodu.</p>
            )}
          </SectionCard>

          <p className="text-right text-xs text-slate-600">
            Generisano: {formatGeneratedAt(insights.generatedAt)}
          </p>
        </div>
      )}
    </div>
  )
}
