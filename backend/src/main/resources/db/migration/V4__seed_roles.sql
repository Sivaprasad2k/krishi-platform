INSERT INTO krishi.roles
(
    id,
    name,
    description
)
VALUES
    (
        gen_random_uuid(),
        'FARMER',
        'Farmer user of the platform'
    ),
    (
        gen_random_uuid(),
        'ADMIN',
        'System administrator'
    );