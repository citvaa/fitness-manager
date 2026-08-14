import { useEffect, useState } from 'react'
import { getMyPaymentStatus, getMyPayments } from './api'
import { PaymentStatusSummary } from './PaymentStatusSummary'
import type { PaymentDTO, SessionTypePaymentStatusDTO } from './types'
import { LoadingIndicator } from '../../components/LoadingIndicator'

const SESSION_TYPE_LABEL: Record<string, string> = { INDIVIDUAL: 'Individualni', GROUP: 'Grupni' }

export function MyPaymentsPage() {
  const [payments, setPayments] = useState<PaymentDTO[]>([])
  const [status, setStatus] = useState<SessionTypePaymentStatusDTO[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getMyPayments(), getMyPaymentStatus()])
      .then(([p, s]) => {
        setPayments(p)
        setStatus(s)
      })
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="p-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-100">Moje uplate</h1>
      <p className="mb-6 text-sm text-slate-500">Istorija tvojih uplata za termine.</p>

      {!loading && (
        <div className="mb-6">
          <PaymentStatusSummary status={status} />
        </div>
      )}

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        {loading ? (
          <LoadingIndicator className="text-sm text-slate-500" />
        ) : payments.length === 0 ? (
          <p className="text-sm text-slate-500">Još nema evidentiranih uplata.</p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-800 text-xs uppercase tracking-wide text-slate-500">
                <th className="py-2 pr-4">Datum</th>
                <th className="py-2 pr-4">Tip sesije</th>
                <th className="py-2 pr-4">Plaćeni termini</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((p) => (
                <tr key={p.id} className="border-b border-slate-800/60">
                  <td className="py-2 pr-4 text-slate-300">{p.paymentDate}</td>
                  <td className="py-2 pr-4 text-slate-400">
                    {SESSION_TYPE_LABEL[p.session.type] ?? p.session.type}
                  </td>
                  <td className="py-2 pr-4 text-slate-400">{p.paidAppointments}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
