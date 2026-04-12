ALTER TABLE event_applications
    DROP CONSTRAINT IF EXISTS uc_9f7bc78c88d9ff6e0905e693d;

-- add partial unique
CREATE UNIQUE INDEX uq_event_applications_active
    ON event_applications (session_id, volunteer_id)
    WHERE status <> 'CANCELLED';