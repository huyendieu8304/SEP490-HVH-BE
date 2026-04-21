-- 1. drop unique constraint (auto-created index)
ALTER TABLE event_applications
DROP CONSTRAINT IF EXISTS ukjrfk87fwivkuub2mino64jycw;

-- 2. drop partial unique index
DROP INDEX IF EXISTS uq_event_applications_active;