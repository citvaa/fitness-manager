import type { PaymentStatus } from '../types'

const labels={INDIVIDUAL:'individualnih',GROUP:'grupnih'} as const

export function PaymentStatusSummary({statuses}:{statuses:PaymentStatus[]}){
  const totalOwed=statuses.reduce((sum,status)=>sum+status.owed,0)
  return <section className="payment-status-summary" aria-label="Status plaćanja">
    <div className="card-head"><div><p className="eyebrow">Iskorišćeni termini</p><h2>Status plaćanja</h2></div></div>
    <div className="payment-status-grid">{statuses.map(status=><article key={status.type}><strong>{status.type==='INDIVIDUAL'?'Individualni':'Grupni'}</strong><p>Plaćeno {Math.min(status.paid,status.held)}/{status.held} {labels[status.type]} termina</p>{status.owed>0&&<small className="payment-debt">Duguje {status.owed} termina</small>}</article>)}</div>
    {totalOwed===0&&<p className="payment-clear">✓ Nema duga</p>}
  </section>
}
