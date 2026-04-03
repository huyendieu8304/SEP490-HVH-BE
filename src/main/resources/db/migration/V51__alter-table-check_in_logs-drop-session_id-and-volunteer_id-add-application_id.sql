ALTER TABLE check_in_logs
DROP
CONSTRAINT fk_check_in_logs_on_session;

ALTER TABLE check_in_logs
DROP
CONSTRAINT fk_check_in_logs_on_volunteer;

ALTER TABLE check_in_logs
    ADD application_id UUID;

ALTER TABLE check_in_logs
    ADD CONSTRAINT FK_CHECK_IN_LOGS_ON_APPLICATION FOREIGN KEY (application_id) REFERENCES event_applications (id);

CREATE INDEX idx_check_in_logs_applicationid ON check_in_logs (application_id);

ALTER TABLE check_in_logs
DROP
COLUMN session_id;

ALTER TABLE check_in_logs
DROP
COLUMN volunteer_id;