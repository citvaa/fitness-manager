import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { RoomOccupancyInsight, SessionTypeInsight } from './types'
import { RATING_COLOR, RATING_LABEL } from './types'

export function RatingBadge({ rating }: { rating: keyof typeof RATING_LABEL }) {
  const color = RATING_COLOR[rating]
  return (
    <span
      className="shrink-0 rounded-full px-2 py-0.5 text-xs font-semibold"
      style={{ backgroundColor: color + '26', color }}
    >
      {RATING_LABEL[rating]}
    </span>
  )
}

export function StatTile({ label, value, suffix }: { label: string; value: string | number; suffix?: string }) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/60 px-4 py-3">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="text-xl font-bold text-slate-100">
        {value}
        {suffix && <span className="ml-1 text-sm font-normal text-slate-500">{suffix}</span>}
      </p>
    </div>
  )
}

const CHART_TOOLTIP_STYLE = {
  contentStyle: { background: '#e6edfb', border: '1px solid #cdd8ea', borderRadius: 8 },
  labelStyle: { color: '#13161d' },
  itemStyle: { color: '#13161d' },
}

// Value labels are rendered outside the bar (to the right), not inside it - a bar can be too
// short to fit text at a small value, and #13161d (same dark ink as the rest of the light theme)
// stays readable against the chart's light surface either way, unlike text drawn over a fill color.
const VALUE_LABEL_STYLE = { fill: '#13161d', fontSize: 12, fontWeight: 600 }

export function RoomOccupancyChart({ rooms }: { rooms: RoomOccupancyInsight[] }) {
  const data = [...rooms].sort((a, b) => b.checkIns - a.checkIns)

  return (
    <ResponsiveContainer width="100%" height={Math.max(160, data.length * 44)}>
      <BarChart data={data} layout="vertical" margin={{ left: 8, right: 36 }}>
        <CartesianGrid stroke="#cdd8ea" strokeDasharray="3 3" horizontal={false} />
        <XAxis type="number" stroke="#7e8ea5" fontSize={12} allowDecimals={false} />
        <YAxis type="category" dataKey="roomName" stroke="#505f74" fontSize={12} width={110} />
        <Tooltip
          {...CHART_TOOLTIP_STYLE}
          formatter={(value) => [`${value} prijava`, 'Broj check-in-a']}
        />
        <Bar dataKey="checkIns" radius={[0, 4, 4, 0]} maxBarSize={22}>
          {data.map((room) => (
            <Cell key={room.roomName} fill={RATING_COLOR[room.rating]} />
          ))}
          <LabelList dataKey="checkIns" position="right" style={VALUE_LABEL_STYLE} />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}

export function SessionTypeChart({ sessions }: { sessions: SessionTypeInsight[] }) {
  return (
    <ResponsiveContainer width="100%" height={Math.max(120, sessions.length * 56)}>
      <BarChart data={sessions} layout="vertical" margin={{ left: 8, right: 36 }}>
        <CartesianGrid stroke="#cdd8ea" strokeDasharray="3 3" horizontal={false} />
        <XAxis type="number" stroke="#7e8ea5" fontSize={12} unit="%" domain={[0, 100]} />
        <YAxis type="category" dataKey="sessionType" stroke="#505f74" fontSize={12} width={90} />
        <Tooltip
          {...CHART_TOOLTIP_STYLE}
          formatter={(value, _name, item) => [
            `${value}% (${(item.payload as SessionTypeInsight).paidAppointments} plaćenih termina)`,
            'Udeo',
          ]}
        />
        <Bar dataKey="sharePercent" radius={[0, 4, 4, 0]} maxBarSize={26}>
          {sessions.map((s) => (
            <Cell key={s.sessionType} fill={RATING_COLOR[s.rating]} />
          ))}
          <LabelList
            dataKey="sharePercent"
            position="right"
            style={VALUE_LABEL_STYLE}
            formatter={(value) => `${value}%`}
          />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}
