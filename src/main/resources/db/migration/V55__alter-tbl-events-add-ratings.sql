ALTER TABLE events
    ADD avg_rating SMALLINT DEFAULT 0;

ALTER TABLE events
    ADD rating_count BIGINT DEFAULT 0;

ALTER TABLE events
    ALTER COLUMN avg_rating SET NOT NULL;

ALTER TABLE events
    ALTER COLUMN rating_count SET NOT NULL;