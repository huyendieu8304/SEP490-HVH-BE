ALTER TABLE volunteers
    ADD avg_rating SMALLINT DEFAULT 0;

ALTER TABLE volunteers
    ADD rating_count BIGINT DEFAULT 0;

ALTER TABLE volunteers
    ALTER COLUMN avg_rating SET NOT NULL;

ALTER TABLE volunteers
    ALTER COLUMN rating_count SET NOT NULL;