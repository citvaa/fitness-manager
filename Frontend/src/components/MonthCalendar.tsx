import { useState } from 'react'

const WEEKDAY_LABELS = ['Pon', 'Uto', 'Sre', 'Čet', 'Pet', 'Sub', 'Ned']
const MONTH_LABELS = [
  'Januar', 'Februar', 'Mart', 'April', 'Maj', 'Jun',
  'Jul', 'Avgust', 'Septembar', 'Oktobar', 'Novembar', 'Decembar',
]

function toIsoDate(year: number, month: number, day: number): string {
  const mm = String(month + 1).padStart(2, '0')
  const dd = String(day).padStart(2, '0')
  return `${year}-${mm}-${dd}`
}

function todayIso(): string {
  const now = new Date()
  return toIsoDate(now.getFullYear(), now.getMonth(), now.getDate())
}

/**
 * A month-grid day picker, replacing the "select a date, then scroll a giant flat list" pattern
 * that both the admin Termini tab and /manager/dnevni-raspored used before the manager-testing
 * round 3 restructure (see AGENTS.md). Deliberately built from scratch rather than pulling in a
 * date-picker dependency - the codebase had none, and the actual need here (pick one day, see
 * which days have data) is small enough not to justify one.
 */
export function MonthCalendar({
  value,
  onChange,
  highlightedDates,
}: {
  value: string
  onChange: (isoDate: string) => void
  /** ISO ("YYYY-MM-DD") dates that should get a dot indicator - e.g. days that already have
   * appointments/schedule entries. */
  highlightedDates?: Set<string>
}) {
  const initial = value ? new Date(value + 'T00:00:00') : new Date()
  const [viewYear, setViewYear] = useState(initial.getFullYear())
  const [viewMonth, setViewMonth] = useState(initial.getMonth())

  const firstOfMonth = new Date(viewYear, viewMonth, 1)
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate()
  // getDay(): 0=Sunday..6=Saturday - shift so the grid starts on Monday.
  const leadingBlanks = (firstOfMonth.getDay() + 6) % 7

  const cells: (number | null)[] = [
    ...Array.from({ length: leadingBlanks }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ]

  const today = todayIso()

  function goToPrevMonth() {
    if (viewMonth === 0) {
      setViewYear((y) => y - 1)
      setViewMonth(11)
    } else {
      setViewMonth((m) => m - 1)
    }
  }

  function goToNextMonth() {
    if (viewMonth === 11) {
      setViewYear((y) => y + 1)
      setViewMonth(0)
    } else {
      setViewMonth((m) => m + 1)
    }
  }

  return (
    <div className="w-full max-w-sm rounded-2xl border border-slate-800 bg-slate-950/60 p-3">
      <div className="mb-2 flex items-center justify-between">
        <button
          type="button"
          onClick={goToPrevMonth}
          className="rounded-lg px-2 py-1 text-sm text-slate-400 hover:bg-slate-800 hover:text-slate-100"
          aria-label="Prethodni mesec"
        >
          ‹
        </button>
        <span className="text-sm font-medium text-slate-200">
          {MONTH_LABELS[viewMonth]} {viewYear}
        </span>
        <button
          type="button"
          onClick={goToNextMonth}
          className="rounded-lg px-2 py-1 text-sm text-slate-400 hover:bg-slate-800 hover:text-slate-100"
          aria-label="Sledeći mesec"
        >
          ›
        </button>
      </div>
      <div className="grid grid-cols-7 gap-1 text-center text-[11px] text-slate-500">
        {WEEKDAY_LABELS.map((label) => (
          <span key={label}>{label}</span>
        ))}
      </div>
      <div className="mt-1 grid grid-cols-7 gap-1">
        {cells.map((day, idx) => {
          if (day === null) return <span key={`blank-${idx}`} />
          const iso = toIsoDate(viewYear, viewMonth, day)
          const isSelected = iso === value
          const isToday = iso === today
          const hasItems = highlightedDates?.has(iso) ?? false
          return (
            <button
              key={iso}
              type="button"
              onClick={() => onChange(iso)}
              className={[
                'relative rounded-lg py-1.5 text-xs transition-colors',
                isSelected
                  ? 'bg-brand-600 text-white'
                  : isToday
                    ? 'bg-slate-800 text-slate-100'
                    : 'text-slate-300 hover:bg-slate-800',
              ].join(' ')}
            >
              {day}
              {hasItems && (
                <span
                  className={[
                    'absolute bottom-0.5 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full',
                    isSelected ? 'bg-white' : 'bg-brand-400',
                  ].join(' ')}
                />
              )}
            </button>
          )
        })}
      </div>
    </div>
  )
}
