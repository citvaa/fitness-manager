import type { RoomType } from './types'
import { ROOM_TYPE_LABEL } from './types'

/**
 * Computes the smallest a room's width/height (in geometry units/"meters", 1 unit = 20px, see
 * PX_PER_UNIT in RoomEditorPage/LiveFloorPlanPage) may be while still guaranteeing that
 * LiveFloorPlanPage's RoomTile renders its full content - icon + name, type label, progress bar,
 * and "count/capacity" + percent badge row - without truncating the name or spilling outside the
 * rectangle. RoomTile has no `overflow-hidden`, so this is a real correctness requirement, not
 * just cosmetic: it is the only thing standing between a too-small room and visibly broken UI.
 *
 * The minimum is derived from the room's actual content (name length is the dominant factor,
 * since it is the only unbounded string) rather than a single fixed constant for every room - see
 * AGENTS.md ("Upgrade: room minimum-size decisions"). Width is measured with Canvas
 * `measureText` against the exact font/weight RoomTile renders with, so it tracks font changes on
 * that page automatically. Height has no free-text component (type label, bar and badge row all
 * have fixed heights and the name never wraps - it's `truncate`, single line) so it comes out as a
 * constant derived from those fixed rows, not zero-content-dependent by choice.
 *
 * `RoomServiceImpl` on the backend mirrors this with a simpler average-character-width heuristic
 * (no canvas/font access server-side) tuned to be at least as strict as this - see its Javadoc.
 */

const PX_PER_UNIT = 20

// RoomTile (LiveFloorPlanPage.tsx) layout constants - keep in sync if that component's classes change.
const TILE_PADDING_PX = 12 // p-3, all sides
const TILE_BORDER_PX = 2 // border-2, each side
const NAME_FONT = '600 14px ui-sans-serif, system-ui, -apple-system, "Segoe UI", sans-serif' // text-sm font-semibold
const TYPE_FONT = '400 11px ui-sans-serif, system-ui, -apple-system, "Segoe UI", sans-serif' // text-[11px]
const BADGE_FONT = '700 12px ui-sans-serif, system-ui, -apple-system, "Segoe UI", sans-serif' // text-xs font-bold
const PERCENT_FONT = '400 12px ui-sans-serif, system-ui, -apple-system, "Segoe UI", sans-serif' // text-xs
const ICON_WIDTH_PX = 18 // emoji glyph box at ~14px font size
const ICON_NAME_GAP_PX = 6 // gap-1.5 between icon and name
const NAME_LINE_HEIGHT_PX = 20 // text-sm default line-height (1.25rem)
const TYPE_LINE_HEIGHT_PX = 16 // text-[11px] default (normal) line-height
const BAR_HEIGHT_PX = 6 // h-1.5
const BAR_MARGIN_BOTTOM_PX = 6 // mb-1.5
const BADGE_HPADDING_PX = 16 // px-2, both sides
const BADGE_VPADDING_PX = 4 // py-0.5, both sides (contributes to bottom-row height, not width)
const BOTTOM_ROW_HEIGHT_PX = 20 // badge/percent row: text-xs line-height + badge vertical padding
const BOTTOM_ROW_GAP_PX = 8 // minimum breathing room between the badge and the percent (justify-between)
// Widest plausible values for the two live-updating pieces of text, so the computed minimum
// doesn't shrink again once a room actually fills up or its occupancy math produces "100%".
const WIDEST_BADGE_SAMPLE = '99/99'
const WIDEST_PERCENT_SAMPLE = '100%'

let measureCanvasCtx: CanvasRenderingContext2D | null | undefined
function measureTextWidth(text: string, font: string): number {
  if (measureCanvasCtx === undefined) {
    measureCanvasCtx =
      typeof document === 'undefined' ? null : document.createElement('canvas').getContext('2d')
  }
  if (!measureCanvasCtx) return text.length * 7 // non-browser fallback (tests/SSR)
  measureCanvasCtx.font = font
  return measureCanvasCtx.measureText(text).width
}

export interface MinRoomUnits {
  minWidthUnits: number
  minHeightUnits: number
}

/** Rounds up to the nearest 0.5 unit so the editor's drag handles snap to human-friendly sizes. */
function roundUpToHalfUnit(px: number): number {
  return Math.ceil((px / PX_PER_UNIT) * 2) / 2
}

export function computeMinRoomUnits(name: string, type: RoomType): MinRoomUnits {
  const trimmedName = name.trim() || 'Sala'
  const typeLabel = ROOM_TYPE_LABEL[type] ?? ''

  const nameRowWidthPx = ICON_WIDTH_PX + ICON_NAME_GAP_PX + measureTextWidth(trimmedName, NAME_FONT)
  const typeRowWidthPx = measureTextWidth(typeLabel, TYPE_FONT)
  const badgeWidthPx = measureTextWidth(WIDEST_BADGE_SAMPLE, BADGE_FONT) + BADGE_HPADDING_PX
  const percentWidthPx = measureTextWidth(WIDEST_PERCENT_SAMPLE, PERCENT_FONT)
  const bottomRowWidthPx = badgeWidthPx + BOTTOM_ROW_GAP_PX + percentWidthPx

  const contentWidthPx = Math.max(nameRowWidthPx, typeRowWidthPx, bottomRowWidthPx)
  const minWidthPx = TILE_PADDING_PX * 2 + TILE_BORDER_PX * 2 + contentWidthPx

  const minHeightPx =
    TILE_PADDING_PX * 2 +
    TILE_BORDER_PX * 2 +
    NAME_LINE_HEIGHT_PX +
    TYPE_LINE_HEIGHT_PX +
    BAR_HEIGHT_PX +
    BAR_MARGIN_BOTTOM_PX +
    Math.max(BOTTOM_ROW_HEIGHT_PX, BADGE_VPADDING_PX + 16)

  return {
    minWidthUnits: roundUpToHalfUnit(minWidthPx),
    minHeightUnits: roundUpToHalfUnit(minHeightPx),
  }
}
