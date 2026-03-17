ALTER TABLE event_sessions
    ADD approved_application_count INTEGER;

UPDATE event_sessions
SET approved_application_count = 0
WHERE approved_application_count IS NULL;

ALTER TABLE event_sessions
    ALTER COLUMN approved_application_count SET NOT NULL;