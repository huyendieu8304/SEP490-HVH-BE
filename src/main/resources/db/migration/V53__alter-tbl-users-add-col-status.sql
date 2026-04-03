ALTER TABLE users
    ADD status VARCHAR(20);

UPDATE users SET status = 'ACTIVE';

ALTER TABLE users
    ALTER COLUMN status SET NOT NULL;