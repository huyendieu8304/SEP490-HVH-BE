ALTER TABLE organizations
    ADD credit_hour INTEGER;

ALTER TABLE organizations
    ALTER COLUMN credit_hour SET NOT NULL;