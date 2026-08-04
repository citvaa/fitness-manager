ALTER TABLE appointment ADD COLUMN room_id INT REFERENCES room(id);
CREATE INDEX idx_appointment_room_date ON appointment(room_id, date);
