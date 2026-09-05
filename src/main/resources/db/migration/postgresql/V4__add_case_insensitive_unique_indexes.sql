DROP INDEX users_user_name_unique;
DROP INDEX users_email_address_unique;

CREATE UNIQUE INDEX users_user_name_unique_lower
    ON users (LOWER(user_name)) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX users_email_address_unique_lower
    ON users (LOWER(email_address)) WHERE deleted_at IS NULL;