import { useEffect, useState } from 'react'
import { getMyNotificationPreference, updateMyNotificationPreference } from './api'
import type { NotificationPreference } from './types'

const LABELS: Record<NotificationPreference, string> = {
  EMAIL: 'Email',
  PUSH: 'Push (u aplikaciji)',
  BOTH: 'Email i push',
}

/** Self-service control for every role (client/trainer/manager) - previously
 * PATCH /{id}/notification-preference only let a MANAGER change *another* user's preference;
 * there was no "my preference" endpoint or UI at all. See AGENTS.md "Upgrade: notification
 * decisions". */
export function NotificationPreferenceSelect() {
  const [value, setValue] = useState<NotificationPreference | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let cancelled = false
    getMyNotificationPreference().then((pref) => {
      if (!cancelled) setValue(pref)
    })
    return () => {
      cancelled = true
    }
  }, [])

  if (value === null) return null

  async function handleChange(next: NotificationPreference) {
    const previous = value
    setValue(next)
    setSaving(true)
    try {
      await updateMyNotificationPreference(next)
    } catch {
      setValue(previous)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <label className="mb-1 block text-xs text-slate-500">Obaveštenja</label>
      <select
        value={value}
        disabled={saving}
        onChange={(e) => handleChange(e.target.value as NotificationPreference)}
        className="w-full rounded-md border border-slate-800 bg-slate-950 px-2 py-1.5 text-sm text-slate-200 disabled:opacity-60"
      >
        {(Object.keys(LABELS) as NotificationPreference[]).map((key) => (
          <option key={key} value={key}>
            {LABELS[key]}
          </option>
        ))}
      </select>
    </div>
  )
}
