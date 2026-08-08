import { useState } from 'react'
import { deleteEntry, updateEntry } from './api'
import type { ClientProgressEntryDTO } from './types'

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('sr-RS', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

const FIELDS: { key: 'weightKg' | 'bodyFatPercent' | 'waistCm' | 'chestCm' | 'hipCm' | 'thighCm' | 'armCm'; label: string }[] = [
  { key: 'weightKg', label: 'Težina (kg)' },
  { key: 'bodyFatPercent', label: 'Mast (%)' },
  { key: 'waistCm', label: 'Struk (cm)' },
  { key: 'chestCm', label: 'Grudi (cm)' },
  { key: 'hipCm', label: 'Kuk (cm)' },
  { key: 'thighCm', label: 'Butina (cm)' },
  { key: 'armCm', label: 'Ruka (cm)' },
]

function toEditForm(entry: ClientProgressEntryDTO) {
  return {
    entryDate: entry.entryDate,
    weightKg: entry.weightKg?.toString() ?? '',
    bodyFatPercent: entry.bodyFatPercent?.toString() ?? '',
    waistCm: entry.waistCm?.toString() ?? '',
    chestCm: entry.chestCm?.toString() ?? '',
    hipCm: entry.hipCm?.toString() ?? '',
    thighCm: entry.thighCm?.toString() ?? '',
    armCm: entry.armCm?.toString() ?? '',
    notes: entry.notes ?? '',
  }
}

/**
 * Raw measurement history as an editable list - previously entries only ever appeared aggregated
 * into ProgressCharts, with no way for a trainer to correct a typo or remove a bad measurement.
 * See AGENTS.md ("Upgrade: Faza 9 decisions"). Read-only on the client screen (no `editable` prop
 * passed there), matching the existing trainer-writes/client-reads-own-data split.
 */
export function EntriesList({
  clientId,
  entries,
  editable = false,
  onChanged,
}: {
  clientId?: number
  entries: ClientProgressEntryDTO[]
  editable?: boolean
  onChanged?: () => void
}) {
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState(toEditForm({} as ClientProgressEntryDTO))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const sorted = [...entries].sort((a, b) => b.entryDate.localeCompare(a.entryDate))

  function startEdit(entry: ClientProgressEntryDTO) {
    setEditingId(entry.id)
    setEditForm(toEditForm(entry))
    setError(null)
  }

  async function saveEdit(id: number) {
    if (!clientId) return
    setSaving(true)
    setError(null)
    try {
      await updateEntry(id, {
        clientId,
        entryDate: editForm.entryDate,
        weightKg: editForm.weightKg ? Number(editForm.weightKg) : null,
        bodyFatPercent: editForm.bodyFatPercent ? Number(editForm.bodyFatPercent) : null,
        waistCm: editForm.waistCm ? Number(editForm.waistCm) : null,
        chestCm: editForm.chestCm ? Number(editForm.chestCm) : null,
        hipCm: editForm.hipCm ? Number(editForm.hipCm) : null,
        thighCm: editForm.thighCm ? Number(editForm.thighCm) : null,
        armCm: editForm.armCm ? Number(editForm.armCm) : null,
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
    if (!confirm('Obrisati ovo merenje?')) return
    await deleteEntry(id)
    onChanged?.()
  }

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
      <h3 className="mb-3 text-sm font-semibold text-slate-300">Istorija merenja</h3>
      {error && <p className="mb-3 text-xs text-red-400">{error}</p>}
      {sorted.length === 0 ? (
        <p className="text-sm text-slate-500">Još uvek nema unetih merenja.</p>
      ) : (
        <ul className="space-y-2">
          {sorted.map((entry) =>
            editingId === entry.id ? (
              <li key={entry.id} className="rounded-lg border border-slate-800 bg-slate-950 p-3">
                <div className="mb-2 grid grid-cols-2 gap-2 md:grid-cols-4">
                  <label className="block text-xs text-slate-400">
                    Datum
                    <input
                      type="date"
                      value={editForm.entryDate}
                      onChange={(e) => setEditForm((f) => ({ ...f, entryDate: e.target.value }))}
                      className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-2 py-1 text-sm text-slate-100 outline-none focus:border-brand-500"
                    />
                  </label>
                  {FIELDS.map((f) => (
                    <label key={f.key} className="block text-xs text-slate-400">
                      {f.label}
                      <input
                        type="number"
                        step="0.1"
                        value={editForm[f.key]}
                        onChange={(e) => setEditForm((form) => ({ ...form, [f.key]: e.target.value }))}
                        className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-2 py-1 text-sm text-slate-100 outline-none focus:border-brand-500"
                      />
                    </label>
                  ))}
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
                    onClick={() => saveEdit(entry.id)}
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
                key={entry.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-slate-800 bg-slate-950 px-3 py-2"
              >
                <div>
                  <p className="text-sm font-medium text-slate-100">{formatDate(entry.entryDate)}</p>
                  <p className="text-xs text-slate-500">
                    {[
                      entry.weightKg != null && `${entry.weightKg} kg`,
                      entry.bodyFatPercent != null && `${entry.bodyFatPercent}% mast`,
                      entry.waistCm != null && `struk ${entry.waistCm}cm`,
                      entry.chestCm != null && `grudi ${entry.chestCm}cm`,
                      entry.hipCm != null && `kuk ${entry.hipCm}cm`,
                      entry.thighCm != null && `butina ${entry.thighCm}cm`,
                      entry.armCm != null && `ruka ${entry.armCm}cm`,
                    ]
                      .filter(Boolean)
                      .join(' · ') || 'nema izmerenih vrednosti'}
                  </p>
                  {entry.notes && <p className="text-xs text-slate-600">{entry.notes}</p>}
                </div>
                {editable && (
                  <div className="flex gap-2">
                    <button
                      onClick={() => startEdit(entry)}
                      className="rounded-lg border border-slate-700 px-2 py-1 text-xs text-slate-300 hover:bg-slate-800"
                    >
                      Izmeni
                    </button>
                    <button
                      onClick={() => handleDelete(entry.id)}
                      className="rounded-lg border border-red-900/50 px-2 py-1 text-xs text-red-300 hover:bg-red-950/40"
                    >
                      Obriši
                    </button>
                  </div>
                )}
              </li>
            ),
          )}
        </ul>
      )}
    </div>
  )
}
