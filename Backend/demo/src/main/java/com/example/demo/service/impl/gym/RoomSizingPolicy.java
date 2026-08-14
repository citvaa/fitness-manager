package com.example.demo.service.impl.gym;

/**
 * Server-side mirror of the frontend's {@code computeMinRoomUnits} (Frontend/src/features/gym/
 * roomSizing.ts): rejects a room create/update whose width/height are too small for its own name
 * to render without truncating or spilling outside its rectangle on
 * /manager/plan-uzivo (LiveFloorPlanPage's RoomTile has no {@code overflow-hidden}).
 *
 * <p>The backend has no canvas/font-metrics access, so it cannot reproduce the frontend's exact
 * {@code measureText} pixel width. Instead it uses a fixed average-character-width heuristic for
 * Latin text at the same 14px/600-weight name font RoomTile renders with, tuned to be
 * <em>at least</em> as strict as the frontend (so nothing that would pass here could still
 * truncate client-side) - see AGENTS.md ("Upgrade: room minimum-size decisions") for the derivation
 * and the constants shared with the frontend (padding/border/icon/badge/bar dimensions).
 */
final class RoomSizingPolicy {

    private RoomSizingPolicy() {
    }

    private static final double PX_PER_UNIT = 20.0;

    // RoomTile (LiveFloorPlanPage.tsx) layout constants - keep in sync with roomSizing.ts.
    private static final double TILE_PADDING_PX = 12 * 2;
    private static final double TILE_BORDER_PX = 2 * 2;
    private static final double ICON_WIDTH_PX = 18;
    private static final double ICON_NAME_GAP_PX = 6;
    // Average glyph width for Latin text at 600-weight/14px sans-serif, deliberately rounded up
    // from a typical ~7.5px measured average so this heuristic stays at least as strict as the
    // frontend's exact canvas measurement.
    private static final double AVG_CHAR_WIDTH_PX = 8.5;
    private static final double NAME_LINE_HEIGHT_PX = 20;
    private static final double TYPE_LINE_HEIGHT_PX = 16;
    private static final double BAR_HEIGHT_PX = 6;
    private static final double BAR_MARGIN_BOTTOM_PX = 6;
    private static final double BOTTOM_ROW_HEIGHT_PX = 20;
    // Fixed width of the "99/99" badge + gap + "100%" percent text - the widest plausible values
    // for those two live-updating pieces of text (see roomSizing.ts), measured once and hardcoded
    // here since it never depends on room content.
    private static final double BOTTOM_ROW_WIDTH_PX = 82;

    static double minWidthUnits(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) trimmed = "Sala";
        double nameRowWidthPx = ICON_WIDTH_PX + ICON_NAME_GAP_PX + trimmed.length() * AVG_CHAR_WIDTH_PX;
        double contentWidthPx = Math.max(nameRowWidthPx, BOTTOM_ROW_WIDTH_PX);
        double minWidthPx = TILE_PADDING_PX + TILE_BORDER_PX + contentWidthPx;
        return roundUpToHalfUnit(minWidthPx);
    }

    static double minHeightUnits() {
        // No free-text component contributes to height (type label/bar/badge row are fixed-height
        // and the name never wraps - single line, truncated) - see roomSizing.ts's Javadoc.
        double minHeightPx = TILE_PADDING_PX + TILE_BORDER_PX + NAME_LINE_HEIGHT_PX + TYPE_LINE_HEIGHT_PX
                + BAR_HEIGHT_PX + BAR_MARGIN_BOTTOM_PX + BOTTOM_ROW_HEIGHT_PX;
        return roundUpToHalfUnit(minHeightPx);
    }

    private static double roundUpToHalfUnit(double px) {
        return Math.ceil((px / PX_PER_UNIT) * 2) / 2.0;
    }
}
