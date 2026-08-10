import { useState, type FormEvent } from 'react'
import { createRecord } from './api'
import { RECORD_UNITS, RECORD_UNIT_LABEL } from './types'
import type { RecordUnit } from './types'

export function RecordForm({ clientId, onCreated }: { clientId: number; onCreated: () => void }) {
  const [exerciseName, setExerciseName] = useState('')
  const [value, setValue] = useState('')
  const [unit, setUnit] = useState<RecordUnit>('KG')
  const [recordDate, setRecordDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!exerciseName.trim() || !value) return
    setSaving(true)
    setError(null)
    try {
      await createRecord({
        clientId,
        exerciseName: exerciseName.trim(),
        value: Number(value),
        unit,
        recordDate,
        notes: notes || null,
      })
      setExerciseName('')
      setValue('')
      setNotes('')
      onCreated()
    } catch {
      setError('Čuvanje rekorda nije uspelo.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
      <h3 className="mb-3 text-sm font-semibold text-slate-300">Novi lični rekord</h3>

      <label className="mb-3 block text-xs text-slate-400">
        Vežba
        <input
          required
          value={exerciseName}
          onChange={(e) => setExerciseName(e.target.value)}
          placeholder="npr. Bench press"
          className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
        />
      </label>

      <div className="mb-3 grid grid-cols-2 gap-2">
        <label className="block text-xs text-slate-400">
          Vrednost
          <input
            type="number"
            step="0.1"
            required
            value={value}
            onChange={(e) => setValue(e.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
          />
        </label>
        <label className="block text-xs text-slate-400">
          Jedinica
          <select
            value={unit}
            onChange={(e) => setUnit(e.target.value as RecordUnit)}
            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
          >
            {RECORD_UNITS.map((u) => (
              <option key={u} value={u}>
                {RECORD_UNIT_LABEL[u]}
              </option>
            ))}
          </select>
        </label>
      </div>

      <label className="mb-3 block text-xs text-slate-400">
        Datum
        <input
          type="date"
              lang="sr-Latn-RS"
          required
          value={recordDate}
          onChange={(e) => setRecordDate(e.target.value)}
          className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
        />
      </label>

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
        {saving ? 'Čuvanje...' : 'Sačuvaj rekord'}
      </button>
    </form>
  )
}
