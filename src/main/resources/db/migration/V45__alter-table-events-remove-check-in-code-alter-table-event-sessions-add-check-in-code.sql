ALTER TABLE event_sessions
    ADD check_in_code VARCHAR(6);

ALTER TABLE events
DROP
COLUMN check_in_code;