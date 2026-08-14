import { useState } from 'react'
import { useNotifications } from './NotificationContext'

/** Visible proof-of-delivery for PUSH notifications - without this, nothing in the frontend ever
 * rendered a WebSocket notification, so the "PUSH" preference option was a no-op regardless of
 * whether the backend sent anything. See AGENTS.md "Upgrade: notification decisions". */
export function NotificationBell() {
  const { notifications, unreadCount, markAllRead } = useNotifications()
  const [open, setOpen] = useState(false)

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

      {open && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
          <div className="absolute right-0 z-50 mt-2 w-80 rounded-lg border border-slate-800 bg-slate-900 p-2 shadow-xl">
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
        </>
      )}
    </div>
  )
}
