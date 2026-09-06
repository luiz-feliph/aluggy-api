CREATE TYPE role AS ENUM ('USER', 'ADMIN');

CREATE TABLE profile_photos
(
    id  UUID PRIMARY KEY,
    url TEXT NOT NULL
);

CREATE TABLE users
(
    id               UUID PRIMARY KEY,
    role             role                     NOT NULL DEFAULT 'USER',
    user_name        VARCHAR(50) UNIQUE       NOT NULL,
    full_name        VARCHAR(255)             NOT NULL,
    email_address    VARCHAR(255) UNIQUE      NOT NULL,
    password         VARCHAR(255)             NOT NULL,
    contact_number   VARCHAR(15)              NOT NULL,
    description      VARCHAR(255),
    registered_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMP WITH TIME ZONE          DEFAULT NULL,
    profile_photo_id UUID UNIQUE              REFERENCES profile_photos (id) ON DELETE SET NULL
);

CREATE TABLE property_types
(
    id        UUID PRIMARY KEY,
    type_name VARCHAR(80) UNIQUE NOT NULL
);

CREATE TABLE environments
(
    id                          UUID PRIMARY KEY,
    current_number_of_residents integer NOT NULL CHECK (current_number_of_residents >= 0),
    sociability_score           integer CHECK (sociability_score BETWEEN 1 AND 5),
    clean_score                 integer CHECK (clean_score BETWEEN 1 AND 5),
    loud_score                  integer CHECK (loud_score BETWEEN 1 AND 5),
    party_score                 integer CHECK (party_score BETWEEN 1 AND 5),
    is_there_smokers            boolean
);

CREATE TABLE properties
(
    id                   UUID PRIMARY KEY,
    property_type_id     UUID           NOT NULL REFERENCES property_types (id) ON DELETE RESTRICT,
    environment_id       UUID REFERENCES environments (id) ON DELETE RESTRICT,
    price                NUMERIC(10, 2) NOT NULL,
    is_negotiable        boolean        NOT NULL,
    area                 NUMERIC(8, 2),
    number_of_bedrooms   integer        NOT NULL CHECK (number_of_bedrooms >= 0),
    number_of_bathrooms  integer        NOT NULL CHECK (number_of_bathrooms >= 0),
    number_of_garages    integer        NOT NULL CHECK (number_of_garages >= 0),
    has_kitchen          boolean        NOT NULL,
    has_laundry          boolean        NOT NULL,
    is_furnished         boolean        NOT NULL,
    includes_water       boolean        NOT NULL,
    includes_electricity boolean        NOT NULL,
    includes_internet    boolean        NOT NULL
);

CREATE TABLE addresses
(
    id          UUID PRIMARY KEY,
    property_id UUID         NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    street      VARCHAR(255) NOT NULL,
    number      VARCHAR(10),
    district    VARCHAR(100) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(100) NOT NULL,
    zip_code    VARCHAR(10)  NOT NULL
);


CREATE TABLE posts
(
    id           UUID PRIMARY KEY,
    user_id      UUID                     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    property_id  UUID                     NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    is_active    boolean                  NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE          DEFAULT NULL,
    deleted_at   TIMESTAMP WITH TIME ZONE          DEFAULT NULL,
    description TEXT
);

CREATE TABLE post_images
(
    id            UUID PRIMARY KEY,
    post_id       UUID    NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    url           TEXT    NOT NULL,
    display_order integer NOT NULL
);

