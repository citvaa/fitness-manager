import { useEffect, useState } from 'react'
import { getEntriesForClient, getInsightForClient, getMyClients, getRecordsForClient } from './api'
import { EntryForm } from './EntryForm'
import { RecordForm } from './RecordForm'
import { ProgressCharts } from './ProgressCharts'
import { PersonalRecordsList } from './PersonalRecordsList'
import { InsightPanel } from './InsightPanel'
import type {
  ClientPersonalRecordDTO,
  ClientProgressEntryDTO,
  ClientProgressInsightDTO,
  ClientSummaryDTO,
} from './types'

export function TrainerProgressPage() {
  const [clients, setClients] = useState<ClientSummaryDTO[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [loadingClients, setLoadingClients] = useState(true)

  const [entries, setEntries] = useState<ClientProgressEntryDTO[]>([])
  const [records, setRecords] = useState<ClientPersonalRecordDTO[]>([])
  const [insight, setInsight] = useState<ClientProgressInsightDTO | null>(null)
  const [loadingDetail, setLoadingDetail] = useState(false)

  useEffect(() => {
    getMyClients()
      .then((list) => {
        setClients(list)
        if (list.length > 0) setSelectedId(list[0].id)
      })
      .finally(() => setLoadingClients(false))
  }, [])

  async function loadDetail(clientId: number) {
    setLoadingDetail(true)
    try {
      const [e, r, i] = await Promise.all([
        getEntriesForClient(clientId),
        getRecordsForClient(clientId),
        getInsightForClient(clientId).catch(() => null),
      ])
      setEntries(e)
      setRecords(r)
      setInsight(i)
    } finally {
      setLoadingDetail(false)
    }
  }

  useEffect(() => {
    if (selectedId != null) void loadDetail(selectedId)
  }, [selectedId])

  if (loadingClients) {
    return <div className="p-8 text-slate-400">Učitavanje...</div>
  }

  return (
    <div className="flex h-full">
      <aside className="w-64 shrink-0 border-r border-slate-800 bg-slate-900/40 p-4">
        <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-400">
          Moji klijenti ({clients.length})
        </h2>
        {clients.length === 0 ? (
          <p className="text-sm text-slate-500">
            Nemaš još nijednog klijenta - klijent se pojavljuje ovde nakon zajedničkog termina.
          </p>
        ) : (
          <ul className="space-y-1">
            {clients.map((c) => (
              <li key={c.id}>
                <button
                  onClick={() => setSelectedId(c.id)}
                  className={`w-full truncate rounded-lg px-3 py-2 text-left text-sm transition ${
                    c.id === selectedId
                      ? 'bg-slate-800 text-white'
                      : 'text-slate-400 hover:bg-slate-800/60'
                  }`}
                >
                  {c.email}
                </button>
              </li>
            ))}
          </ul>
        )}
      </aside>

      <div className="flex-1 overflow-auto p-6">
        {!selectedId ? (
          <p className="text-sm text-slate-500">Izaberi klijenta sa leve strane.</p>
        ) : loadingDetail ? (
          <div className="text-slate-400">Učitavanje...</div>
        ) : (
          <div className="space-y-6">
            <h1 className="text-lg font-semibold text-slate-100">
              Napredak — {clients.find((c) => c.id === selectedId)?.email}
            </h1>

            <ProgressCharts entries={entries} />

            <div className="grid gap-4 md:grid-cols-2">
              <EntryForm clientId={selectedId} onCreated={() => void loadDetail(selectedId)} />
              <RecordForm clientId={selectedId} onCreated={() => void loadDetail(selectedId)} />
            </div>

            <PersonalRecordsList records={records} />

            <InsightPanel
              insight={insight}
              loading={false}
              onRefresh={async () => {
                setInsight(await getInsightForClient(selectedId))
              }}
            />
          </div>
        )}
      </div>
    </div>
  )
}
