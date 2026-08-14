ALTER TABLE users DROP CONSTRAINT users_user_name_key;
ALTER TABLE users DROP CONSTRAINT users_email_address_key;

CREATE UNIQUE INDEX users_user_name_unique
    ON users (user_name) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX users_email_address_unique
    ON users (email_address) WHERE deleted_at IS NULL;