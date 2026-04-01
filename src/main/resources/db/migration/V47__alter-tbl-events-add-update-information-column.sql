ALTER TABLE events
    ADD update_critical BOOLEAN;

ALTER TABLE events
    ADD update_event_payload JSONB;