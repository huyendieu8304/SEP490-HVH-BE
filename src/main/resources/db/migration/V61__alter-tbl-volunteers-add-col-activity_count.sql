ALTER TABLE volunteers
    ADD activity_count INTEGER DEFAULT 0;

ALTER TABLE volunteers
    ALTER COLUMN activity_count SET NOT NULL;