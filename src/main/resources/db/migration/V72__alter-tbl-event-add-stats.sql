ALTER TABLE events
    ADD total_credit_hours INTEGER DEFAULT 0;

ALTER TABLE events
    ADD total_approved_applications INTEGER DEFAULT 0;

ALTER TABLE events
    ADD total_attended_applications INTEGER DEFAULT 0;