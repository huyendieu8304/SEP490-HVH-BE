CREATE TABLE event_claims
(
    id             UUID         NOT NULL,
    application_id UUID         NOT NULL,
    honor_hour     SMALLINT     NOT NULL,
    reason         VARCHAR(100) NOT NULL,
    detail_reason  VARCHAR(300) NOT NULL,
    evidences      VARCHAR(500),
    CONSTRAINT pk_event_claims PRIMARY KEY (id)
);

ALTER TABLE event_claims
    ADD CONSTRAINT uc_event_claims_application UNIQUE (application_id);

ALTER TABLE event_claims
    ADD CONSTRAINT FK_EVENT_CLAIMS_ON_APPLICATION FOREIGN KEY (application_id) REFERENCES event_applications (id);