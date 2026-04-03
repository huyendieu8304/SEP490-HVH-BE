CREATE TABLE event_ratings
(
    id                          UUID                     NOT NULL,
    application_id              UUID                     NOT NULL,
    organization_quality_rating SMALLINT                 NOT NULL,
    professionalism_rating      SMALLINT                 NOT NULL,
    work_environment_rating     SMALLINT                 NOT NULL,
    value_impact_rating         SMALLINT                 NOT NULL,
    support_connection_rating   SMALLINT                 NOT NULL,
    avg_rating                  SMALLINT                 NOT NULL,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_event_ratings PRIMARY KEY (id)
);

ALTER TABLE event_ratings
    ADD CONSTRAINT uc_event_ratings_application UNIQUE (application_id);

ALTER TABLE event_ratings
    ADD CONSTRAINT FK_EVENT_RATINGS_ON_APPLICATION FOREIGN KEY (application_id) REFERENCES event_applications (id);