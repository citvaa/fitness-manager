import { useEffect, useRef, useState } from 'react'

/**
 * Tweens a displayed number from its previous value to `value` over
 * `duration`ms (easeOutCubic) instead of jumping straight to it - used on
 * the live floor plan so occupancy counts/percentages visibly count up/down
 * rather than snapping on every WebSocket update.
 */
export function useAnimatedNumber(value: number, duration = 600): number {
  const [display, setDisplay] = useState(value)
  const fromRef = useRef(value)

  useEffect(() => {
    const from = fromRef.current
    const to = value
    if (from === to) return

    let rafId: number
    const start = performance.now()

    function tick(now: number) {
      const t = Math.min((now - start) / duration, 1)
      const eased = 1 - Math.pow(1 - t, 3)
      setDisplay(from + (to - from) * eased)
      if (t < 1) {
        rafId = requestAnimationFrame(tick)
      } else {
        fromRef.current = to
      }
    }

    rafId = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(rafId)
  }, [value, duration])

  return display
}
