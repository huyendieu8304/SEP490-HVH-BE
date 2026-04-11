ALTER TABLE organizations
    ADD avg_rating SMALLINT DEFAULT 0;

ALTER TABLE organizations
    ADD hosted_event_count INTEGER DEFAULT 0;

ALTER TABLE organizations
    ADD status VARCHAR(20) DEFAULT 'ACTIVE';

ALTER TABLE organizations
    ALTER COLUMN avg_rating SET NOT NULL;

ALTER TABLE organizations
    ALTER COLUMN hosted_event_count SET NOT NULL;

ALTER TABLE organizations
    ALTER COLUMN status SET NOT NULL;