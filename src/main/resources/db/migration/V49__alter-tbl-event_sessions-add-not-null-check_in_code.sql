-- fill toàn bộ bằng random 6 chữ số
UPDATE event_sessions
SET check_in_code = LPAD(FLOOR(RANDOM() * 1000000)::text, 6, '0')
WHERE check_in_code is null
;

ALTER TABLE event_sessions
    ALTER COLUMN check_in_code SET NOT NULL;

