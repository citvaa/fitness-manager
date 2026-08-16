import { useEffect, useState } from 'react'
import { subscribeToActiveRequests } from '../lib/http'
import { Spinner } from './LoadingIndicator'

/**
 * Small floating "something is happening" indicator, driven by lib/http.ts's global in-flight
 * request counter - complements the per-page LoadingIndicator (which only ever covers the fetch
 * that page itself kicked off) with a cross-cutting signal for any background call, mutation, or
 * another panel's refresh. Mounted once in AppShell, fixed to the bottom-right corner so it never
 * shifts page layout. See AGENTS.md "Upgrade: global activity indicator decisions".
 */
export function GlobalActivityIndicator() {
  const [count, setCount] = useState(0)

  useEffect(() => subscribeToActiveRequests(setCount), [])

  if (count === 0) return null

  return (
    <div className="pointer-events-none fixed bottom-4 right-4 z-50 flex items-center gap-2 rounded-full border border-slate-800 bg-slate-900/90 px-3 py-1.5 text-xs text-slate-300 shadow-lg">
      <Spinner className="h-3.5 w-3.5 shrink-0" />
      <span>Učitavanje...</span>
    </div>
  )
}
