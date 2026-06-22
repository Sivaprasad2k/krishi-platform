CREATE TABLE IF NOT EXISTS krishi.roles
(
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
    );