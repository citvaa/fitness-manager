import { useSyncExternalStore } from 'react'
import { loadingStore } from '../state/loadingStore'

export function LoadingIndicator({label='Učitavanje…',page=false}:{label?:string;page?:boolean}) {
  return <div className={page?'loading-page shared-loading':'shared-loading'} role="status" aria-live="polite"><span className="loading-spinner" aria-hidden="true"/><span>{label}</span></div>
}

export function GlobalLoadingIndicator() {
  const pending = useSyncExternalStore(loadingStore.subscribe,loadingStore.getSnapshot,loadingStore.getSnapshot)
  return pending>0?<div className="global-loading" role="status" aria-live="polite"><span className="loading-spinner" aria-hidden="true"/><span>Sačekajte…</span></div>:null
}
