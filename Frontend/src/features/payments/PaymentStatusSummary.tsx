import type { SessionTypePaymentStatusDTO } from './types'

const SESSION_TYPE_LABEL: Record<string, string> = { INDIVIDUAL: 'individualnih', GROUP: 'grupnih' }

/**
 * "Plaćeno X/Y individualnih, Z/W grupnih" summary with a clear debt callout - shared between
 * MyPaymentsPage.tsx (CLIENT, own status) and ManagerPaymentsPage.tsx (MANAGER, selected client's
 * status). `held` (actually-occurred past appointments) vs `paid` (Payment.paidAppointments), both
 * per SessionType - see AGENTS.md "Upgrade: payment debt tracking decisions". A client who has
 * paid for more than they've held (common - paying ahead of attending) shows paid/held, not an
 * "overpaid" warning; only held > paid triggers the debt callout.
 */
export function PaymentStatusSummary({ status }: { status: SessionTypePaymentStatusDTO[] }) {
  const totalOwed = status.reduce((sum, s) => sum + s.owed, 0)
  // A type with neither held nor paid appointments has nothing to report - skip it rather than
  // showing a noisy "Plaćeno 0/0" row.
  const relevant = status.filter((s) => s.held > 0 || s.paid > 0)

  if (relevant.length === 0) {
    return (
      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="mb-3 text-sm font-semibold text-slate-300">Status plaćanja</h3>
        <p className="text-sm text-slate-500">Još nema održanih termina ni uplata.</p>
      </div>
    )
  }

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
      <h3 className="mb-3 text-sm font-semibold text-slate-300">Status plaćanja</h3>
      <div className="grid gap-3 sm:grid-cols-2">
        {relevant.map((s) => (
          <div key={s.type} className="rounded-lg bg-slate-950/60 px-3 py-2 text-sm">
            <p className="text-slate-300">
              Plaćeno {Math.min(s.paid, s.held)}/{s.held} {SESSION_TYPE_LABEL[s.type] ?? s.type}
            </p>
            {s.owed > 0 && <p className="mt-1 text-xs text-red-400">Duguje {s.owed} termina</p>}
          </div>
        ))}
      </div>
      {totalOwed === 0 && (
        <p className="mt-3 text-xs text-emerald-400">Nema duga - sve održane termine je pokrila uplata.</p>
      )}
    </div>
  )
}
