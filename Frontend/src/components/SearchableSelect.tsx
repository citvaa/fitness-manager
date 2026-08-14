import { useEffect, useRef, useState } from 'react'

export interface SearchableSelectOption {
  value: number
  label: string
}

/**
 * A searchable/filterable dropdown - replaces plain `<select>` lists that became unusable once
 * the dev seeder started generating 50 clients (see AGENTS.md "Upgrade: fixed weekly appointment
 * decisions" / manager-testing round 3). No combobox library existed in this codebase
 * (`@headlessui/react`, `downshift`, etc. were never dependencies), so this is a small
 * from-scratch implementation rather than a new dependency, matching the scope of the actual need
 * (type to filter a flat option list, no multi-select/async loading required).
 */
export function SearchableSelect({
  options,
  value,
  onChange,
  placeholder = 'Pretraži...',
  required,
}: {
  options: SearchableSelectOption[]
  value: number | ''
  onChange: (value: number | '') => void
  placeholder?: string
  required?: boolean
}) {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const containerRef = useRef<HTMLDivElement>(null)

  const selected = options.find((o) => o.value === value) ?? null

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
        setQuery('')
      }
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  const filtered = query.trim()
    ? options.filter((o) => o.label.toLowerCase().includes(query.trim().toLowerCase()))
    : options

  return (
    <div ref={containerRef} className="relative">
      <input
        type="text"
        required={required && !selected}
        value={open ? query : selected?.label ?? ''}
        placeholder={placeholder}
        onFocus={() => {
          setOpen(true)
          setQuery('')
        }}
        onChange={(e) => setQuery(e.target.value)}
        className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-2 py-1.5 text-sm text-slate-100 outline-none focus:border-brand-500"
      />
      {open && (
        <ul className="absolute z-20 mt-1 max-h-56 w-full overflow-y-auto rounded-lg border border-slate-700 bg-slate-900 shadow-lg">
          {selected && (
            <li>
              <button
                type="button"
                onClick={() => {
                  onChange('')
                  setOpen(false)
                  setQuery('')
                }}
                className="w-full px-2 py-1.5 text-left text-xs text-slate-500 hover:bg-slate-800"
              >
                Očisti izbor
              </button>
            </li>
          )}
          {filtered.length === 0 ? (
            <li className="px-2 py-1.5 text-xs text-slate-500">Nema rezultata.</li>
          ) : (
            filtered.map((o) => (
              <li key={o.value}>
                <button
                  type="button"
                  onClick={() => {
                    onChange(o.value)
                    setOpen(false)
                    setQuery('')
                  }}
                  className={[
                    'w-full px-2 py-1.5 text-left text-sm hover:bg-brand-600 hover:text-white',
                    o.value === value ? 'bg-slate-800 text-brand-300' : 'text-slate-200',
                  ].join(' ')}
                >
                  {o.label}
                </button>
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  )
}
