-- Bumps any existing room row below the room editor's client-side minimum resize floor
-- (4m x 2.5m, RoomEditorPage.tsx MIN_ROOM_WIDTH_UNITS/MIN_ROOM_HEIGHT_UNITS, commit 72403c3) up
-- to that minimum. That earlier fix only prevents *new* shrinking below the floor in the editor
-- UI - it does nothing for rooms that were already smaller than that before the fix landed (e.g.
-- "Recepcija", resized during manual testing), which still visually overflow the room name
-- label/occupancy count on both the room editor and the live floor plan.
--
-- Written as an UPDATE (not an INSERT) because it must also repair rows on databases that
-- already ran V1.0016 and were then manually resized through the UI - not just seed a fresh
-- database. Idempotent: after the first run every room satisfies both minimums, so the WHERE
-- clause matches nothing on subsequent runs.
UPDATE room
SET width = 4.0
WHERE width < 4.0;

UPDATE room
SET height = 2.5
WHERE height < 2.5;
