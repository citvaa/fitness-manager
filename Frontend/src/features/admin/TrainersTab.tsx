import { type FormEvent, useEffect, useState } from 'react'
import { createTrainer, deleteTrainer, getTrainers, updateTrainer } from './api'
import { ActivationLinkBanner } from './ActivationLinkBanner'
import { TrainerScheduleManager } from './TrainerScheduleManager'
import type { EmploymentStatus, TrainerDTO } from './types'

const STATUS_LABEL: Record<EmploymentStatus, string> = {
  FULL_TIME: 'Puno radno vreme',
  CONTRACT: 'Ugovor',
  FORMER_EMPLOYEE: 'Bivši zaposleni',
}

const EMPTY_FORM = {
  email: '',
  employmentDate: new Date().toISOString().slice(0, 10),
  birthYear: new Date().getFullYear() - 30,
  status: 'FULL_TIME' as EmploymentStatus,
}

export function TrainersTab() {
  const [trainers, setTrainers] = useState<TrainerDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [expandedId, setExpandedId] = useState<number | null>(null)

  const [form, setForm] = useState(EMPTY_FORM)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [lastCreatedKey, setLastCreatedKey] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState(EMPTY_FORM)

  async function reload() {
    setLoading(true)
    try {
      setTrainers(await getTrainers())
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
  }, [])

  async function handleCreate(e: FormEvent) {
    e.preventDefault()
    setCreating(true)
    setCreateError(null)
    setLastCreatedKey(null)
    try {
      const created = await createTrainer(form)
      setForm(EMPTY_FORM)
      setLastCreatedKey(created.user.registrationKey)
      await reload()
    } catch {
      setCreateError('Kreiranje trenera nije uspelo - proveri da email već ne postoji.')
    } finally {
      setCreating(false)
    }
  }

  function startEdit(t: TrainerDTO) {
    setEditingId(t.id)
    setEditForm({
      email: t.user.email,
      employmentDate: t.employmentDate,
      birthYear: t.birthYear,
      status: t.status,
    })
  }

  async function saveEdit(id: number) {
    await updateTrainer(id, editForm)
    setEditingId(null)
    await reload()
  }

  async function handleDelete(id: number) {
    if (!confirm('Obrisati ovog trenera? Ovo briše i njegov raspored.')) return
    await deleteTrainer(id)
    await reload()
  }

  return (
    <div className="space-y-6">
      <form
        onSubmit={handleCreate}
        className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4"
      >
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Novi trener</h3>
        <div className="grid gap-3 md:grid-cols-4">
          <label className="block text-xs text-slate-400">
            Email
            <input
              type="email"
              required
              value={form.email}
              onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
          <label className="block text-xs text-slate-400">
            Datum zaposlenja
            <input
              type="date"
              required
              value={form.employmentDate}
              onChange={(e) => setForm((f) => ({ ...f, employmentDate: e.target.value }))}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
          <label className="block text-xs text-slate-400">
            Godina rođenja
            <input
              type="number"
              required
              value={form.birthYear}
              onChange={(e) => setForm((f) => ({ ...f, birthYear: Number(e.target.value) }))}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            />
          </label>
          <label className="block text-xs text-slate-400">
            Status
            <select
              value={form.status}
              onChange={(e) => setForm((f) => ({ ...f, status: e.target.value as EmploymentStatus }))}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
            >
              {Object.entries(STATUS_LABEL).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
        </div>
        <button
          type="submit"
          disabled={creating}
          className="mt-3 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-500 disabled:opacity-60"
        >
          {creating ? 'Kreiranje...' : 'Kreiraj trenera'}
        </button>
        {createError && <p className="mt-3 text-xs text-red-400">{createError}</p>}
        {lastCreatedKey && (
          <div className="mt-3">
            <ActivationLinkBanner registrationKey={lastCreatedKey} />
          </div>
        )}
      </form>

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Treneri ({trainers.length})</h3>
        {loading ? (
          <p className="text-sm text-slate-500">Učitavanje...</p>
        ) : trainers.length === 0 ? (
          <p className="text-sm text-slate-500">Nema trenera.</p>
        ) : (
          <ul className="space-y-2">
            {trainers.map((t) => (
              <li key={t.id} className="rounded-xl border border-slate-800 p-3">
                {editingId === t.id ? (
                  <div className="grid gap-2 md:grid-cols-4">
                    <input
                      value={editForm.email}
                      onChange={(e) => setEditForm((f) => ({ ...f, email: e.target.value }))}
                      className="rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
                    />
                    <input
                      type="date"
                      value={editForm.employmentDate}
                      onChange={(e) =>
                        setEditForm((f) => ({ ...f, employmentDate: e.target.value }))
                      }
                      className="rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
                    />
                    <input
                      type="number"
                      value={editForm.birthYear}
                      onChange={(e) =>
                        setEditForm((f) => ({ ...f, birthYear: Number(e.target.value) }))
                      }
                      className="rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
                    />
                    <select
                      value={editForm.status}
                      onChange={(e) =>
                        setEditForm((f) => ({ ...f, status: e.target.value as EmploymentStatus }))
                      }
                      className="rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
                    >
                      {Object.entries(STATUS_LABEL).map(([value, label]) => (
                        <option key={value} value={value}>
                          {label}
                        </option>
                      ))}
                    </select>
                    <div className="flex gap-2 md:col-span-4">
                      <button
                        onClick={() => saveEdit(t.id)}
                        className="rounded-lg bg-brand-600 px-3 py-1 text-xs text-white hover:bg-brand-500"
                      >
                        Sačuvaj
                      </button>
                      <button
                        onClick={() => setEditingId(null)}
                        className="rounded-lg border border-slate-700 px-3 py-1 text-xs text-slate-300 hover:bg-slate-800"
                      >
                        Otkaži
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <p className="text-sm font-medium text-slate-100">{t.user.email}</p>
                      <p className="text-xs text-slate-500">
                        {STATUS_LABEL[t.status]} · zaposlen {t.employmentDate} · rođen{' '}
                        {t.birthYear}
                      </p>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => setExpandedId(expandedId === t.id ? null : t.id)}
                        className="rounded-lg border border-slate-700 px-2 py-1 text-xs text-slate-300 hover:bg-slate-800"
                      >
                        {expandedId === t.id ? 'Sakrij raspored' : 'Raspored'}
                      </button>
                      <button
                        onClick={() => startEdit(t)}
                        className="rounded-lg border border-slate-700 px-2 py-1 text-xs text-slate-300 hover:bg-slate-800"
                      >
                        Izmeni
                      </button>
                      <button
                        onClick={() => handleDelete(t.id)}
                        className="rounded-lg border border-red-900/50 px-2 py-1 text-xs text-red-300 hover:bg-red-950/40"
                      >
                        Obriši
                      </button>
                    </div>
                  </div>
                )}

                {expandedId === t.id && <TrainerScheduleManager trainerId={t.id} />}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
