import { useState, type FormEvent } from 'react'
import { createEntry } from './api'

const FIELDS: { key: keyof typeof EMPTY_NUMS; label: string }[] = [
  { key: 'weightKg', label: 'Težina (kg)' },
  { key: 'bodyFatPercent', label: 'Mast (%)' },
  { key: 'waistCm', label: 'Struk (cm)' },
  { key: 'chestCm', label: 'Grudi (cm)' },
  { key: 'hipCm', label: 'Kuk (cm)' },
  { key: 'thighCm', label: 'Butina (cm)' },
  { key: 'armCm', label: 'Ruka (cm)' },
]

const EMPTY_NUMS = {
  weightKg: '',
  bodyFatPercent: '',
  waistCm: '',
  chestCm: '',
  hipCm: '',
  thighCm: '',
  armCm: '',
}

export function EntryForm({ clientId, onCreated }: { clientId: number; onCreated: () => void }) {
  const [entryDate, setEntryDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [values, setValues] = useState(EMPTY_NUMS)
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await createEntry({
        clientId,
        entryDate,
        weightKg: values.weightKg ? Number(values.weightKg) : null,
        bodyFatPercent: values.bodyFatPercent ? Number(values.bodyFatPercent) : null,
        waistCm: values.waistCm ? Number(values.waistCm) : null,
        chestCm: values.chestCm ? Number(values.chestCm) : null,
        hipCm: values.hipCm ? Number(values.hipCm) : null,
        thighCm: values.thighCm ? Number(values.thighCm) : null,
        armCm: values.armCm ? Number(values.armCm) : null,
        notes: notes || null,
      })
      setValues(EMPTY_NUMS)
      setNotes('')
      onCreated()
    } catch {
      setError('Čuvanje merenja nije uspelo.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
      <h3 className="mb-3 text-sm font-semibold text-slate-300">Novo merenje</h3>

      <label className="mb-3 block text-xs text-slate-400">
        Datum
        <input
          type="date"
          required
          value={entryDate}
          onChange={(e) => setEntryDate(e.target.value)}
          className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
        />
      </label>

      <div className="mb-3 grid grid-cols-2 gap-2">
        {FIELDS.map((f) => (
          <label key={f.key} className="block text-xs text-slate-400">
            {f.label}
            <input
              type="number"
              step="0.1"
              value={values[f.key]}
              onChange={(e) => setValues((v) => ({ ...v, [f.key]: e.target.value }))}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
        ))}
      </div>

      <label className="mb-3 block text-xs text-slate-400">
        Napomena
        <input
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
        />
      </label>

      {error && <p className="mb-3 text-xs text-red-400">{error}</p>}

      <button
        type="submit"
        disabled={saving}
        className="w-full rounded-lg bg-brand-600 px-3 py-2 text-sm font-medium text-white hover:bg-brand-500 disabled:opacity-60"
      >
        {saving ? 'Čuvanje...' : 'Sačuvaj merenje'}
      </button>
    </form>
  )
}
