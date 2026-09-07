import type { ReactNode } from 'react'
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { ClientProgressEntryDTO } from './types'

// Same brand/occupancy palette as the rest of the app (see index.css @theme) - kept as plain
// hex here since Recharts needs literal color values, not Tailwind classes.
const LINE_COLORS = {
  weight: '#2f83fb', // brand-500
  bodyFat: '#a85100', // occ-busy
  waist: '#008e27', // occ-free
  chest: '#7458c7',
  hip: '#ff7272', // occ-full
  thigh: '#0072ad',
  arm: '#13161d',
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('sr-RS', { day: '2-digit', month: '2-digit' })
}

function ChartCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
      <h3 className="mb-3 text-sm font-semibold text-slate-300">{title}</h3>
      {children}
    </div>
  )
}

export function ProgressCharts({ entries }: { entries: ClientProgressEntryDTO[] }) {
  const data = [...entries]
    .sort((a, b) => a.entryDate.localeCompare(b.entryDate))
    .map((e) => ({
      date: formatDate(e.entryDate),
      Težina: e.weightKg,
      'Mast %': e.bodyFatPercent,
      Struk: e.waistCm,
      Grudi: e.chestCm,
      Kuk: e.hipCm,
      Butina: e.thighCm,
      Ruka: e.armCm,
    }))

  if (data.length === 0) {
    return (
      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-6 text-sm text-slate-500">
        Još uvek nema unetih merenja.
      </div>
    )
  }

  return (
    <div className="grid gap-4 md:grid-cols-2">
      <ChartCard title="Telesna masa i procenat masti">
        <ResponsiveContainer width="100%" height={240}>
          <LineChart data={data}>
            <CartesianGrid stroke="#cdd8ea" strokeDasharray="3 3" />
            <XAxis dataKey="date" stroke="#7e8ea5" fontSize={12} />
            <YAxis stroke="#7e8ea5" fontSize={12} />
            <Tooltip
              contentStyle={{ background: '#e6edfb', border: '1px solid #cdd8ea', borderRadius: 8 }}
              labelStyle={{ color: '#13161d' }}
            />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            <Line type="monotone" dataKey="Težina" stroke={LINE_COLORS.weight} connectNulls dot={{ r: 3 }} />
            <Line type="monotone" dataKey="Mast %" stroke={LINE_COLORS.bodyFat} connectNulls dot={{ r: 3 }} />
          </LineChart>
        </ResponsiveContainer>
      </ChartCard>

      <ChartCard title="Obimi tela (cm)">
        <ResponsiveContainer width="100%" height={240}>
          <LineChart data={data}>
            <CartesianGrid stroke="#cdd8ea" strokeDasharray="3 3" />
            <XAxis dataKey="date" stroke="#7e8ea5" fontSize={12} />
            <YAxis stroke="#7e8ea5" fontSize={12} />
            <Tooltip
              contentStyle={{ background: '#e6edfb', border: '1px solid #cdd8ea', borderRadius: 8 }}
              labelStyle={{ color: '#13161d' }}
            />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            <Line type="monotone" dataKey="Struk" stroke={LINE_COLORS.waist} connectNulls dot={{ r: 3 }} />
            <Line type="monotone" dataKey="Grudi" stroke={LINE_COLORS.chest} connectNulls dot={{ r: 3 }} />
            <Line type="monotone" dataKey="Kuk" stroke={LINE_COLORS.hip} connectNulls dot={{ r: 3 }} />
            <Line type="monotone" dataKey="Butina" stroke={LINE_COLORS.thigh} connectNulls dot={{ r: 3 }} />
            <Line type="monotone" dataKey="Ruka" stroke={LINE_COLORS.arm} connectNulls dot={{ r: 3 }} />
          </LineChart>
        </ResponsiveContainer>
      </ChartCard>
    </div>
  )
}
