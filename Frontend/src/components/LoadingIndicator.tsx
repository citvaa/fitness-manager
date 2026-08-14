import clsx from 'clsx'

/**
 * A small rotating-circle spinner, styled via `currentColor` so it always matches whatever text
 * color class the caller applies (`text-slate-400`/`text-slate-500` etc., consistent with the
 * app's dark theme) rather than needing its own color prop. See AGENTS.md "Upgrade: shared
 * loading-indicator decisions".
 */
export function Spinner({ className }: { className?: string }) {
  return (
    <svg
      className={clsx('animate-spin', className)}
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path
        className="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 0 1 8-8V0C5.373 0 0 5.373 0 12h4z"
      />
    </svg>
  )
}

/**
 * Drop-in replacement for the app's ~23 bare `Učitavanje...` loading messages (`grep
 * "Učitavanje..." Frontend/src` before this component existed) - those had no visual indicator
 * that anything was actually happening, just static text. `className` is passed straight through
 * so every call site keeps its existing text size/color/spacing (`text-sm text-slate-500`,
 * `p-8 text-slate-400`, etc.) unchanged; only the element itself becomes a spinner+text row
 * instead of a bare `<p>`/`<div>`.
 */
export function LoadingIndicator({
  label = 'Učitavanje...',
  className,
}: {
  label?: string
  className?: string
}) {
  return (
    <div className={clsx('flex items-center gap-2', className)}>
      <Spinner className="h-4 w-4 shrink-0" />
      <span>{label}</span>
    </div>
  )
}
