-- fill NULL = 'UNSPECIFIED'
UPDATE events
SET served_target = 'UNSPECIFIED'
WHERE served_target IS NULL;

UPDATE events
SET serving_place_type = 'OTHER'
WHERE serving_place_type IS NULL;

-- add constraint
ALTER TABLE events
    ALTER COLUMN served_target SET NOT NULL;

ALTER TABLE events
    ALTER COLUMN serving_place_type SET NOT NULL;