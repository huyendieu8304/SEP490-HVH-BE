ALTER TABLE users
    ADD face_registered BOOLEAN DEFAULT FALSE;

ALTER TABLE users
    ALTER COLUMN face_registered SET NOT NULL;