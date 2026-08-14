import { useEffect, useState } from 'react'
import { getMyEntries, getMyInsight, getMyRecords } from './api'
import { ProgressCharts } from './ProgressCharts'
import { EntriesList } from './EntriesList'
import { PersonalRecordChart, PersonalRecordsList } from './PersonalRecordsList'
import { InsightPanel } from './InsightPanel'
import type { ClientPersonalRecordDTO, ClientProgressEntryDTO, ClientProgressInsightDTO } from './types'

export function ClientProgressPage() {
  const [entries, setEntries] = useState<ClientProgressEntryDTO[]>([])
  const [records, setRecords] = useState<ClientPersonalRecordDTO[]>([])
  const [insight, setInsight] = useState<ClientProgressInsightDTO | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    const [e, r, i] = await Promise.all([
      getMyEntries().catch(() => []),
      getMyRecords().catch(() => []),
      getMyInsight().catch(() => null),
    ])
    setEntries(e)
    setRecords(r)
    setInsight(i)
    setLoading(false)
  }

  useEffect(() => {
    void load()
  }, [])

  if (loading) {
    return <div className="p-8 text-slate-400">Učitavanje...</div>
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-6">
      <div>
        <h1 className="text-lg font-semibold text-slate-100">Moj napredak</h1>
        <p className="text-sm text-slate-500">
          Merenja i lične rekorde unosi tvoj trener nakon treninga - ovde ih pratiš.
        </p>
      </div>

      <ProgressCharts entries={entries} />
      <PersonalRecordChart records={records} />
      <EntriesList entries={entries} />
      <PersonalRecordsList records={records} />
      <InsightPanel insight={insight} loading={false} onRefresh={async () => setInsight(await getMyInsight())} />
    </div>
  )
}
