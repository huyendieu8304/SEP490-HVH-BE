CREATE TABLE check_in_logs
(
    id                       UUID                     NOT NULL,
    volunteer_id             UUID,
    session_id               UUID,
    device_id                VARCHAR(255)             NOT NULL,
    ap_version               VARCHAR(255)             NOT NULL,
    os_version               VARCHAR(255)             NOT NULL,
    check_in_location        GEOGRAPHY(Point, 4326)   NOT NULL,
    check_in_accuracy_meters DOUBLE PRECISION         NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_check_in_logs PRIMARY KEY (id)
);

ALTER TABLE check_in_logs
    ADD CONSTRAINT uc_d1d73ca861eb5105518ecfdad UNIQUE (session_id, volunteer_id);

CREATE INDEX idx_check_in_logs_volunterid_sessionid ON check_in_logs (volunteer_id, session_id);

ALTER TABLE check_in_logs
    ADD CONSTRAINT FK_CHECK_IN_LOGS_ON_SESSION FOREIGN KEY (session_id) REFERENCES event_sessions (id);

ALTER TABLE check_in_logs
    ADD CONSTRAINT FK_CHECK_IN_LOGS_ON_VOLUNTEER FOREIGN KEY (volunteer_id) REFERENCES volunteers (id);