CREATE TABLE IF NOT EXISTS people
(
    id         BIGSERIAL PRIMARY KEY NOT NULL,
    name       VARCHAR(255)          NOT NULL,
    age        INT
);
