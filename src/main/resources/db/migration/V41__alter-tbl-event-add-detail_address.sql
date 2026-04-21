-- 1. add column
ALTER TABLE events
    ADD COLUMN detail_address VARCHAR(255);

-- 2. copy data từ address
UPDATE events
SET detail_address = address;

-- 3. set NOT NULL
ALTER TABLE events
    ALTER COLUMN detail_address SET NOT NULL;