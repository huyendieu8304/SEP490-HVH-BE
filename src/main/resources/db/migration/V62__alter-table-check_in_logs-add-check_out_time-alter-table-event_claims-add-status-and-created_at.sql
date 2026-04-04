ALTER TABLE check_in_logs
    ADD check_out_time TIMESTAMP WITH TIME ZONE;

ALTER TABLE check_in_logs
    ALTER COLUMN check_out_time SET NOT NULL;

ALTER TABLE event_claims
    ADD created_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE event_claims
    ADD status VARCHAR(30);

ALTER TABLE event_claims
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE event_claims
    ALTER COLUMN status SET NOT NULL;