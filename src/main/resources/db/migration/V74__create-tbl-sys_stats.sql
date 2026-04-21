CREATE TABLE sys_stats
(
    id                     UUID                     NOT NULL,
    year                   INTEGER                  NOT NULL,
    month                  INTEGER                  NOT NULL,
    verified_volunteers    INTEGER,
    verified_organizations INTEGER,
    completed_events       INTEGER,
    credit_hours           INTEGER,
    approved_applications  INTEGER,
    attended_applications  INTEGER,
    count_event_in_domain  JSONB,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_sys_stats PRIMARY KEY (id)
);

ALTER TABLE sys_stats
    ADD CONSTRAINT sys_stats_year_month UNIQUE (year, month);