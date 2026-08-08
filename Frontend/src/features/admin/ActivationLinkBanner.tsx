import { useState } from 'react'

/**
 * Shows the full activation link after a manager creates a user/trainer/client, since real
 * activation emails aren't sent in this environment (MAIL_USERNAME/MAIL_PASSWORD in .env aren't
 * a real Gmail app password yet). This is a deliberate, documented dev/demo affordance - not a
 * hack to hide - see AGENTS.md "Upgrade: Faza 6 decisions". In production this box would not
 * exist; the same link would only ever reach the user via the activation email.
 */
export function ActivationLinkBanner({ registrationKey }: { registrationKey: string }) {
  const [copied, setCopied] = useState(false)
  const link = `${window.location.origin}/register/complete?registration_key=${registrationKey}`

  async function copy() {
    try {
      await navigator.clipboard.writeText(link)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      // Clipboard API can be unavailable (e.g. insecure context) - the link is still visible to select manually.
    }
  }

  return (
    <div className="rounded-lg border border-amber-900/50 bg-amber-950/30 p-3">
      <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-amber-400">
        Dev/demo način - u produkciji ovo ide isključivo emailom
      </p>
      <p className="mb-2 break-all rounded bg-slate-950 px-2 py-1.5 font-mono text-xs text-slate-300">
        {link}
      </p>
      <button
        type="button"
        onClick={copy}
        className="rounded-lg border border-slate-700 px-3 py-1 text-xs text-slate-300 hover:bg-slate-800"
      >
        {copied ? 'Kopirano ✓' : 'Kopiraj link'}
      </button>
    </div>
  )
}
