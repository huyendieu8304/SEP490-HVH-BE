CREATE TABLE event_applications
(
    id           UUID                     NOT NULL,
    volunteer_id UUID,
    session_id   UUID,
    status       VARCHAR(30)              NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_event_applications PRIMARY KEY (id)
);

ALTER TABLE event_applications
    ADD CONSTRAINT uc_9f7bc78c88d9ff6e0905e693d UNIQUE (session_id, volunteer_id);

CREATE INDEX idx_event_applications_volunterid_sessionid ON event_applications (volunteer_id, session_id);

ALTER TABLE event_applications
    ADD CONSTRAINT FK_EVENT_APPLICATIONS_ON_SESSION FOREIGN KEY (session_id) REFERENCES event_sessions (id);

ALTER TABLE event_applications
    ADD CONSTRAINT FK_EVENT_APPLICATIONS_ON_VOLUNTEER FOREIGN KEY (volunteer_id) REFERENCES volunteers (id);