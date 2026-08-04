CREATE UNIQUE INDEX uq_room_check_in_client_open
    ON room_check_in(client_id)
    WHERE checked_out_at IS NULL;
