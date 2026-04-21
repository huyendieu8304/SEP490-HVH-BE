ALTER TABLE event_applications
    ADD session_date date;

ALTER TABLE event_applications
    ALTER COLUMN session_date SET NOT NULL;