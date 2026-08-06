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
  bodyFat: '#f59e0b', // occ-busy
  waist: '#22c55e', // occ-free
  chest: '#a78bfa',
  hip: '#ef4444', // occ-full
  thigh: '#38bdf8',
  arm: '#e6e9f0',
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
            <CartesianGrid stroke="#1e293b" strokeDasharray="3 3" />
            <XAxis dataKey="date" stroke="#64748b" fontSize={12} />
            <YAxis stroke="#64748b" fontSize={12} />
            <Tooltip
              contentStyle={{ background: '#0b1220', border: '1px solid #1e293b', borderRadius: 8 }}
              labelStyle={{ color: '#e6e9f0' }}
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
            <CartesianGrid stroke="#1e293b" strokeDasharray="3 3" />
            <XAxis dataKey="date" stroke="#64748b" fontSize={12} />
            <YAxis stroke="#64748b" fontSize={12} />
            <Tooltip
              contentStyle={{ background: '#0b1220', border: '1px solid #1e293b', borderRadius: 8 }}
              labelStyle={{ color: '#e6e9f0' }}
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
