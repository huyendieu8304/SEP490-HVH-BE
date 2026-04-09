ALTER TABLE organizations
    ADD avg_rating SMALLINT;

ALTER TABLE organizations
    ADD hosted_event_count INTEGER;

ALTER TABLE organizations
    ADD status VARCHAR(20);

ALTER TABLE organizations
    ALTER COLUMN avg_rating SET NOT NULL;

ALTER TABLE organizations
    ALTER COLUMN hosted_event_count SET NOT NULL;

ALTER TABLE organizations
    ALTER COLUMN status SET NOT NULL;