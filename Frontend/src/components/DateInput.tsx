import { useState } from 'react'

/**
 * Wraps a native `<input type="date">` with a readable "Izaberite datum" placeholder overlay.
 *
 * Chromium renders an empty date input's built-in placeholder as unlabeled "dd-----yyyy"-shaped
 * segments, and that placeholder's format is derived from the browser/OS UI language rather than
 * the page's `lang` attribute - so the `lang="sr-Latn-RS"` attribute every date input in this app
 * already carries does NOT localize it (confirmed live in Chrome/Edge during "manager-testing
 * round 2" - see AGENTS.md "Known issues" for the investigation, and "Upgrade: DateInput
 * decisions" for this fix). There is no CSS or `placeholder`-attribute hook into that native
 * placeholder either.
 *
 * The fix here is a separate overlay label, not a CSS trick against the native placeholder: an
 * absolutely-positioned, `pointer-events-none` span showing "Izaberite datum" sits on top of the
 * native input whenever it has no value and isn't focused. `pointer-events-none` means a click
 * always reaches the native input underneath (still opens the picker normally); the overlay
 * disappears the moment the input gets a value (a real date shows through normally) or gets
 * focus (so typing/using the native picker is never obscured).
 */
export function DateInput({
  value,
  onChange,
  required,
  className,
  placeholder = 'Izaberite datum',
  id,
  min,
  max,
}: {
  value: string
  onChange: (value: string) => void
  required?: boolean
  className?: string
  placeholder?: string
  id?: string
  min?: string
  max?: string
}) {
  const [focused, setFocused] = useState(false)
  const showPlaceholder = !value && !focused

  return (
    <div className="relative">
      <input
        id={id}
        type="date"
        lang="sr-Latn-RS"
        required={required}
        min={min}
        max={max}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        className={className}
      />
      {showPlaceholder && (
        <span className="pointer-events-none absolute inset-y-0 left-2 flex items-center text-sm text-slate-500">
          {placeholder}
        </span>
      )}
    </div>
  )
}
