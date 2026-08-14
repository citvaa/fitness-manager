import { useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useNotifications } from './NotificationContext'

const PANEL_WIDTH = 320 // px, matches the panel's w-80
const VIEWPORT_MARGIN = 8

/** Visible proof-of-delivery for PUSH notifications - without this, nothing in the frontend ever
 * rendered a WebSocket notification, so the "PUSH" preference option was a no-op regardless of
 * whether the backend sent anything. See AGENTS.md "Upgrade: notification decisions".
 *
 * The dropdown panel is rendered via a portal into document.body with `position: fixed`
 * coordinates computed from the bell button's own getBoundingClientRect() - it does NOT nest
 * inside the sidebar's `relative` wrapper. AppShell's <aside> is `w-64` (256px) with
 * `overflow-y-auto` for the scroll-containment fix (see AGENTS.md "Upgrade: AppShell
 * scroll-containment fix"); a `w-80` (320px) panel positioned `absolute right-0` inside that
 * narrow column pushed ~64px past its left edge, and browsers clip the non-scrolling axis
 * whenever the other axis has `overflow` set to something other than `visible` - so the panel's
 * left portion (including the "Nema obaveštenja" empty state and the start of every message) was
 * silently cut off. A portal sidesteps this entirely: the panel floats above the whole page,
 * independent of any ancestor's overflow context, the same way a standard Popover/Menu
 * implementation would. */
export function NotificationBell() {
  const { notifications, unreadCount, markAllRead } = useNotifications()
  const [open, setOpen] = useState(false)
  const buttonRef = useRef<HTMLButtonElement>(null)
  const [coords, setCoords] = useState<{ top: number; left: number } | null>(null)

  useLayoutEffect(() => {
    if (!open || !buttonRef.current) return

    function updatePosition() {
      const rect = buttonRef.current!.getBoundingClientRect()
      const left = Math.min(
        Math.max(rect.right - PANEL_WIDTH, VIEWPORT_MARGIN),
        window.innerWidth - PANEL_WIDTH - VIEWPORT_MARGIN,
      )
      setCoords({ top: rect.bottom + 8, left })
    }

    updatePosition()
    window.addEventListener('resize', updatePosition)
    window.addEventListener('scroll', updatePosition, true)
    return () => {
      window.removeEventListener('resize', updatePosition)
      window.removeEventListener('scroll', updatePosition, true)
    }
  }, [open])

  function toggle() {
    setOpen((prev) => {
      const next = !prev
      if (next) markAllRead()
      return next
    })
  }

  return (
    <div className="relative">
      <button
        ref={buttonRef}
        onClick={toggle}
        aria-label="Obaveštenja"
        className="relative rounded-lg p-2 text-lg text-slate-400 transition hover:bg-slate-800 hover:text-slate-200"
      >
        🔔
        {unreadCount > 0 && (
          <span className="absolute right-0.5 top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold text-white">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {open &&
        coords &&
        createPortal(
          <>
            <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
            <div
              style={{ top: coords.top, left: coords.left, width: PANEL_WIDTH }}
              className="fixed z-50 rounded-lg border border-slate-800 bg-slate-900 p-2 shadow-xl"
            >
              {notifications.length === 0 ? (
                <p className="px-2 py-3 text-sm text-slate-500">Nema obaveštenja</p>
              ) : (
                <ul className="max-h-80 space-y-1 overflow-y-auto">
                  {notifications.map((n) => (
                    <li key={n.id} className="rounded-md bg-slate-950/60 px-3 py-2 text-sm text-slate-200">
                      {n.message}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </>,
          document.body,
        )}
    </div>
  )
}
