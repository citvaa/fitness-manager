import { RECORD_UNIT_LABEL } from './types'
import type { ClientPersonalRecordDTO } from './types'

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('sr-RS', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

export function PersonalRecordsList({ records }: { records: ClientPersonalRecordDTO[] }) {
  const sorted = [...records].sort((a, b) => b.recordDate.localeCompare(a.recordDate))

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
      <h3 className="mb-3 text-sm font-semibold text-slate-300">Lični rekordi</h3>
      {sorted.length === 0 ? (
        <p className="text-sm text-slate-500">Još uvek nema unetih rekorda.</p>
      ) : (
        <ul className="space-y-2">
          {sorted.map((r) => (
            <li
              key={r.id}
              className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950 px-3 py-2"
            >
              <div>
                <p className="text-sm font-medium text-slate-100">{r.exerciseName}</p>
                {r.notes && <p className="text-xs text-slate-500">{r.notes}</p>}
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold text-brand-400">
                  {r.value} {RECORD_UNIT_LABEL[r.unit]}
                </p>
                <p className="text-xs text-slate-500">{formatDate(r.recordDate)}</p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
