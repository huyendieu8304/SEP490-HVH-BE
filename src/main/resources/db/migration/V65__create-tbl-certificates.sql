CREATE TABLE certificates
(
    id               UUID                     NOT NULL,
    code             VARCHAR(255)             NOT NULL,
    event_id         UUID                     NOT NULL,
    volunteer_id     UUID                     NOT NULL,
    certificate_path VARCHAR(255)             NOT NULL,
    issued_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    status           VARCHAR(20)              NOT NULL,
    CONSTRAINT pk_certificates PRIMARY KEY (id)
);

ALTER TABLE certificates
    ADD CONSTRAINT uc_certificates_code UNIQUE (code);

ALTER TABLE certificates
    ADD CONSTRAINT uk_certificate_event_volunteer UNIQUE (event_id, volunteer_id);

ALTER TABLE certificates
    ADD CONSTRAINT FK_CERTIFICATES_ON_EVENT FOREIGN KEY (event_id) REFERENCES events (id);

ALTER TABLE certificates
    ADD CONSTRAINT FK_CERTIFICATES_ON_VOLUNTEER FOREIGN KEY (volunteer_id) REFERENCES volunteers (id);

CREATE INDEX idx_certificates_volunteer ON certificates (volunteer_id);