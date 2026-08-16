let pending = 0
const listeners = new Set<() => void>()

const emit = () => listeners.forEach(listener => listener())

export const loadingStore = {
  start() { pending += 1; emit() },
  finish() { pending = Math.max(0, pending - 1); emit() },
  getSnapshot() { return pending },
  subscribe(listener: () => void) { listeners.add(listener); return () => listeners.delete(listener) },
}
