ALTER TABLE check_in_logs
    ADD check_in_time TIMESTAMP WITH TIME ZONE;

ALTER TABLE check_in_logs
    ADD credit_hour SMALLINT;

ALTER TABLE check_in_logs
    ALTER COLUMN check_in_time SET NOT NULL;