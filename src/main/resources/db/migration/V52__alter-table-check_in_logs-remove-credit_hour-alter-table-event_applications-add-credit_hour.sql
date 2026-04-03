ALTER TABLE event_applications
    ADD credit_hour SMALLINT;

ALTER TABLE check_in_logs
DROP
COLUMN credit_hour;