-- 1. update end_date từ session
UPDATE events e
SET end_date = sub.max_date
FROM (
         SELECT
             event_id,
             MAX(end_date_time AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS max_date
         FROM event_sessions
         GROUP BY event_id
     ) sub
WHERE e.id = sub.event_id;

-- 2. check event không có session
DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM events e
                     LEFT JOIN event_sessions s ON e.id = s.event_id
            WHERE s.id IS NULL
        ) THEN
            RAISE EXCEPTION 'Some events have no sessions → cannot set end_date';
        END IF;
    END $$;

-- 3. set NOT NULL
ALTER TABLE events
    ALTER COLUMN end_date SET NOT NULL;