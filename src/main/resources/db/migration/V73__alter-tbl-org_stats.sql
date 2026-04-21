CREATE TABLE org_stats
(
    id                    UUID                     NOT NULL,
    org_id                UUID                     NOT NULL,
    year                  INTEGER                  NOT NULL,
    month                 INTEGER                  NOT NULL,
    completed_events      INTEGER,
    credit_hours          INTEGER,
    approved_applications INTEGER,
    attended_applications INTEGER,
    top_hosts             JSONB,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_org_stats PRIMARY KEY (id)
);

ALTER TABLE org_stats
    ADD CONSTRAINT org_stats_org_id_month_year UNIQUE (org_id, year, month);