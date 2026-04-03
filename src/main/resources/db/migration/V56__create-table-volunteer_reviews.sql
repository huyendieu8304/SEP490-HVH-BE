CREATE TABLE volunteer_reviews
(
    id                                  UUID                     NOT NULL,
    application_id                      UUID                     NOT NULL,
    professional_attitude_rating        SMALLINT                 NOT NULL,
    responsibility_punctuality_rating   SMALLINT                 NOT NULL,
    work_effectiveness_rating           SMALLINT                 NOT NULL,
    teamwork_communication_rating       SMALLINT                 NOT NULL,
    adaptability_problem_solving_rating SMALLINT                 NOT NULL,
    avg_rating                          SMALLINT                 NOT NULL,
    comment                             VARCHAR(250),
    created_at                          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_volunteer_reviews PRIMARY KEY (id)
);

ALTER TABLE volunteer_reviews
    ADD CONSTRAINT uc_volunteer_reviews_application UNIQUE (application_id);

ALTER TABLE volunteer_reviews
    ADD CONSTRAINT FK_VOLUNTEER_REVIEWS_ON_APPLICATION FOREIGN KEY (application_id) REFERENCES event_applications (id);