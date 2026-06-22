CREATE TABLE krishi.farms
(
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    size_hectares DOUBLE PRECISION NOT NULL,
    location VARCHAR(255) NOT NULL,
    owner_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_farm_owner
        FOREIGN KEY (owner_id)
            REFERENCES krishi.users(id)
);
