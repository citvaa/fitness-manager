import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react'

interface ConfirmOptions {
  title?: string
  confirmLabel?: string
  cancelLabel?: string
}

interface ConfirmRequest extends ConfirmOptions {
  message: string
}

type ConfirmFn = (message: string, options?: ConfirmOptions) => Promise<boolean>

const ConfirmContext = createContext<ConfirmFn | null>(null)

/**
 * Promise-based replacement for bare `confirm()` (5 call sites: TrainersTab, UsersTab,
 * EntriesList, PersonalRecordsList, TrainerSchedulePage - all delete-confirmations). Mount once
 * in AppShell, same pattern as NotificationProvider. `useConfirm()` returns an async function
 * that resolves `true`/`false` exactly like `window.confirm` did, so call sites only need to
 * change `if (!confirm(...))` to `if (!(await confirm(...)))`.
 */
export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [request, setRequest] = useState<ConfirmRequest | null>(null)
  const resolveRef = useRef<((value: boolean) => void) | null>(null)

  const confirm = useCallback<ConfirmFn>((message, options) => {
    return new Promise<boolean>((resolve) => {
      resolveRef.current = resolve
      setRequest({ message, ...options })
    })
  }, [])

  function settle(value: boolean) {
    resolveRef.current?.(value)
    resolveRef.current = null
    setRequest(null)
  }

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {request && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-sm rounded-lg border border-slate-800 bg-slate-900 p-5 shadow-xl">
            {request.title && <h2 className="text-base font-semibold text-slate-100">{request.title}</h2>}
            <p className="mt-1 whitespace-pre-line text-sm text-slate-300">{request.message}</p>
            <div className="mt-5 flex justify-end gap-2">
              <button
                onClick={() => settle(false)}
                className="rounded-lg border border-slate-800 px-3 py-1.5 text-sm text-slate-300 transition hover:bg-slate-800"
              >
                {request.cancelLabel ?? 'Otkaži'}
              </button>
              <button
                onClick={() => settle(true)}
                className="rounded-lg bg-red-600 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-red-500"
              >
                {request.confirmLabel ?? 'Potvrdi'}
              </button>
            </div>
          </div>
        </div>
      )}
    </ConfirmContext.Provider>
  )
}

export function useConfirm(): ConfirmFn {
  const ctx = useContext(ConfirmContext)
  if (!ctx) throw new Error('useConfirm must be used within a ConfirmProvider')
  return useMemo(() => ctx, [ctx])
}
