CREATE TABLE event_moments
(
    id              UUID                     NOT NULL,
    application_id  UUID                     NOT NULL,
    moment_pictures VARCHAR(500),
    moment_content  VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_event_moments PRIMARY KEY (id)
);

ALTER TABLE event_moments
    ADD CONSTRAINT uc_event_moments_application UNIQUE (application_id);

ALTER TABLE event_moments
    ADD CONSTRAINT FK_EVENT_MOMENTS_ON_APPLICATION FOREIGN KEY (application_id) REFERENCES event_applications (id);