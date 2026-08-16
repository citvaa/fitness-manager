import { useMemo, useState } from 'react'
import { DateInput } from '../../components/DateInput'
import { useConfirm } from '../../components/ConfirmDialog'
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { deleteRecord, updateRecord } from './api'
import { RECORD_UNIT_LABEL, RECORD_UNITS } from './types'
import type { ClientPersonalRecordDTO, RecordUnit } from './types'

// Same brand color as ProgressCharts.tsx's weight line - kept as a literal hex since Recharts
// needs a real color value, not a Tailwind class.
const CHART_LINE_COLOR = '#2f83fb' // brand-500

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('sr-RS', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function formatShortDate(iso: string) {
  return new Date(iso).toLocaleDateString('sr-RS', { day: '2-digit', month: '2-digit' })
}

/**
 * A record's value over time for ONE chosen exercise - different exercises use different
 * units/scales (kg vs. seconds vs. reps), so they can't share one axis/line the way
 * ProgressCharts.tsx's body-measurement lines can. A dropdown to pick which exercise to chart
 * (defaulting to whichever has the most history) is a smaller UI footprint than a small multiple
 * per exercise, and still lets a client/trainer compare trend shape across exercises by simply
 * switching the selection. See AGENTS.md "Upgrade: personal-records chart decisions".
 *
 * Exported and rendered by the page components (TrainerProgressPage/ClientProgressPage)
 * alongside ProgressCharts at the top of the page, ABOVE the entry/record forms - not rendered
 * from inside PersonalRecordsList below, even though it's defined in this file. See AGENTS.md
 * "Upgrade: progress-page chart placement decisions".
 */
export function PersonalRecordChart({ records }: { records: ClientPersonalRecordDTO[] }) {
  const exerciseNames = useMemo(() => {
    const counts = new Map<string, number>()
    for (const r of records) counts.set(r.exerciseName, (counts.get(r.exerciseName) ?? 0) + 1)
    return [...counts.entries()].sort((a, b) => b[1] - a[1]).map(([name]) => name)
  }, [records])

  const [selected, setSelected] = useState<string | null>(null)
  const activeExercise = selected && exerciseNames.includes(selected) ? selected : (exerciseNames[0] ?? null)

  const data = useMemo(() => {
    if (!activeExercise) return []
    return records
      .filter((r) => r.exerciseName === activeExercise)
      .sort((a, b) => a.recordDate.localeCompare(b.recordDate))
      .map((r) => ({ date: formatShortDate(r.recordDate), value: r.value }))
  }, [records, activeExercise])

  const unit = activeExercise ? records.find((r) => r.exerciseName === activeExercise)?.unit : undefined

  if (exerciseNames.length === 0) return null

  return (
    <div className="mb-4 rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
      <div className="mb-3 flex items-center justify-between gap-2">
        <h3 className="text-sm font-semibold text-slate-300">Grafik ličnog rekorda</h3>
        <select
          value={activeExercise ?? ''}
          onChange={(e) => setSelected(e.target.value)}
          className="rounded-lg border border-slate-700 bg-slate-950 px-2 py-1 text-xs text-slate-100 outline-none focus:border-brand-500"
        >
          {exerciseNames.map((name) => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
        </select>
      </div>
      {data.length < 2 ? (
        <p className="text-sm text-slate-500">Potrebno je bar dva unosa za ovu vežbu da bi se video grafik.</p>
      ) : (
        <ResponsiveContainer width="100%" height={220}>
          <LineChart data={data}>
            <CartesianGrid stroke="#1e293b" strokeDasharray="3 3" />
            <XAxis dataKey="date" stroke="#64748b" fontSize={12} />
            <YAxis stroke="#64748b" fontSize={12} unit={unit ? ` ${RECORD_UNIT_LABEL[unit]}` : undefined} />
            <Tooltip
              contentStyle={{ background: '#0b1220', border: '1px solid #1e293b', borderRadius: 8 }}
              labelStyle={{ color: '#e6e9f0' }}
              formatter={(value) => [`${value} ${unit ? RECORD_UNIT_LABEL[unit] : ''}`, activeExercise]}
            />
            <Line type="monotone" dataKey="value" stroke={CHART_LINE_COLOR} dot={{ r: 3 }} />
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}

function toEditForm(record: ClientPersonalRecordDTO) {
  return {
    exerciseName: record.exerciseName,
    value: String(record.value),
    unit: record.unit,
    recordDate: record.recordDate,
    notes: record.notes ?? '',
  }
}

/**
 * Editable on the trainer screen (`editable`), read-only on the client's own screen (no
 * `editable` prop passed there) - added in Faza 9, see AGENTS.md ("Upgrade: Faza 9 decisions").
 */
export function PersonalRecordsList({
  clientId,
  records,
  editable = false,
  onChanged,
}: {
  clientId?: number
  records: ClientPersonalRecordDTO[]
  editable?: boolean
  onChanged?: () => void
}) {
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState(toEditForm({ unit: 'KG' } as ClientPersonalRecordDTO))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const confirm = useConfirm()

  const sorted = [...records].sort((a, b) => b.recordDate.localeCompare(a.recordDate))

  function startEdit(record: ClientPersonalRecordDTO) {
    setEditingId(record.id)
    setEditForm(toEditForm(record))
    setError(null)
  }

  async function saveEdit(id: number) {
    if (!clientId) return
    if (!editForm.exerciseName.trim() || !editForm.value) return
    setSaving(true)
    setError(null)
    try {
      await updateRecord(id, {
        clientId,
        exerciseName: editForm.exerciseName.trim(),
        value: Number(editForm.value),
        unit: editForm.unit,
        recordDate: editForm.recordDate,
        notes: editForm.notes || null,
      })
      setEditingId(null)
      onChanged?.()
    } catch {
      setError('Čuvanje izmene nije uspelo.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (!(await confirm('Obrisati ovaj rekord?'))) return
    await deleteRecord(id)
    onChanged?.()
  }

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
      <h3 className="mb-3 text-sm font-semibold text-slate-300">Lični rekordi</h3>
      {error && <p className="mb-3 text-xs text-red-400">{error}</p>}
      {sorted.length === 0 ? (
        <p className="text-sm text-slate-500">Još uvek nema unetih rekorda.</p>
      ) : (
        <ul className="space-y-2">
          {sorted.map((r) =>
            editingId === r.id ? (
              <li key={r.id} className="rounded-lg border border-slate-800 bg-slate-950 p-3">
                <div className="mb-2 grid grid-cols-2 gap-2 md:grid-cols-4">
                  <label className="block text-xs text-slate-400 md:col-span-2">
                    Vežba
                    <input
                      value={editForm.exerciseName}
                      onChange={(e) => setEditForm((f) => ({ ...f, exerciseName: e.target.value }))}
                      className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-2 py-1 text-sm text-slate-100 outline-none focus:border-brand-500"
                    />
                  </label>
                  <label className="block text-xs text-slate-400">
                    Vrednost
                    <input
                      type="number"
                      step="0.1"
                      value={editForm.value}
                      onChange={(e) => setEditForm((f) => ({ ...f, value: e.target.value }))}
                      className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-2 py-1 text-sm text-slate-100 outline-none focus:border-brand-500"
                    />
                  </label>
                  <label className="block text-xs text-slate-400">
                    Jedinica
                    <select
                      value={editForm.unit}
                      onChange={(e) => setEditForm((f) => ({ ...f, unit: e.target.value as RecordUnit }))}
                      className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-2 py-1 text-sm text-slate-100 outline-none focus:border-brand-500"
                    >
                      {RECORD_UNITS.map((u) => (
                        <option key={u} value={u}>
                          {RECORD_UNIT_LABEL[u]}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="block text-xs text-slate-400">
                    Datum
                    <DateInput
                      value={editForm.recordDate}
                      onChange={(v) => setEditForm((f) => ({ ...f, recordDate: v }))}
                      className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-2 py-1 text-sm text-slate-100 outline-none focus:border-brand-500"
                    />
                  </label>
                  <label className="block text-xs text-slate-400 md:col-span-2">
                    Napomena
                    <input
                      value={editForm.notes}
                      onChange={(e) => setEditForm((f) => ({ ...f, notes: e.target.value }))}
                      className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-2 py-1 text-sm text-slate-100 outline-none focus:border-brand-500"
                    />
                  </label>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => saveEdit(r.id)}
                    disabled={saving}
                    className="rounded-lg bg-brand-600 px-3 py-1 text-xs text-white hover:bg-brand-500 disabled:opacity-60"
                  >
                    {saving ? 'Čuvanje...' : 'Sačuvaj'}
                  </button>
                  <button
                    onClick={() => setEditingId(null)}
                    className="rounded-lg border border-slate-700 px-3 py-1 text-xs text-slate-300 hover:bg-slate-800"
                  >
                    Otkaži
                  </button>
                </div>
              </li>
            ) : (
              <li
                key={r.id}
                className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950 px-3 py-2"
              >
                <div>
                  <p className="text-sm font-medium text-slate-100">{r.exerciseName}</p>
                  {r.notes && <p className="text-xs text-slate-500">{r.notes}</p>}
                </div>
                <div className="flex items-center gap-3">
                  <div className="text-right">
                    <p className="text-sm font-semibold text-brand-400">
                      {r.value} {RECORD_UNIT_LABEL[r.unit]}
                    </p>
                    <p className="text-xs text-slate-500">{formatDate(r.recordDate)}</p>
                  </div>
                  {editable && (
                    <div className="flex gap-2">
                      <button
                        onClick={() => startEdit(r)}
                        className="rounded-lg border border-slate-700 px-2 py-1 text-xs text-slate-300 hover:bg-slate-800"
                      >
                        Izmeni
                      </button>
                      <button
                        onClick={() => handleDelete(r.id)}
                        className="rounded-lg border border-red-900/50 px-2 py-1 text-xs text-red-300 hover:bg-red-950/40"
                      >
                        Obriši
                      </button>
                    </div>
                  )}
                </div>
              </li>
            ),
          )}
        </ul>
      )}
    </div>
  )
}
