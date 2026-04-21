ALTER TABLE event_sessions
    ALTER COLUMN check_in_code DROP NOT NULL;

UPDATE event_sessions SET check_in_code = NULL;

ALTER TABLE event_sessions
    ADD CONSTRAINT uc_event_sessions_check_in_code UNIQUE (check_in_code);