import { useEffect, useState } from 'react'
import { getMyPayments } from './api'
import type { PaymentDTO } from './types'

const SESSION_TYPE_LABEL: Record<string, string> = { INDIVIDUAL: 'Individualni', GROUP: 'Grupni' }

export function MyPaymentsPage() {
  const [payments, setPayments] = useState<PaymentDTO[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getMyPayments()
      .then(setPayments)
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="p-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-100">Moje uplate</h1>
      <p className="mb-6 text-sm text-slate-500">Istorija tvojih uplata za termine.</p>

      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        {loading ? (
          <p className="text-sm text-slate-500">Učitavanje...</p>
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
