ALTER TABLE events
    ADD serving_activity BOOLEAN;

UPDATE events
SET serving_activity = TRUE;

ALTER TABLE events
    ALTER COLUMN serving_activity SET NOT NULL;