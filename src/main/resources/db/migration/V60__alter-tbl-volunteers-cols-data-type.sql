ALTER TABLE volunteers
    DROP COLUMN credit_score;

ALTER TABLE volunteers
    DROP COLUMN honor_score;

ALTER TABLE volunteers
    DROP COLUMN rating_count;

ALTER TABLE volunteers
    ADD credit_score INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE volunteers
    ADD honor_score INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE volunteers
    ADD rating_count INTEGER DEFAULT 0 NOT NULL;