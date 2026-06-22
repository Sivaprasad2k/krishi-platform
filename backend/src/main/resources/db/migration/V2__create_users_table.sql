CREATE TABLE krishi.users
(
    id UUID PRIMARY KEY,

    phone_number VARCHAR(10) NOT NULL UNIQUE,

    email VARCHAR(255) UNIQUE,

    password_hash VARCHAR(255) NOT NULL,

    full_name VARCHAR(150) NOT NULL,

    role_id UUID NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    created_by UUID,

    updated_by UUID,

    CONSTRAINT fk_user_role
        FOREIGN KEY (role_id)
            REFERENCES krishi.roles(id)
);