ALTER TABLE organizations
    ADD credit_hour INTEGER;

UPDATE organizations
SET credit_hour = 0;

ALTER TABLE organizations
    ALTER COLUMN credit_hour SET NOT NULL;