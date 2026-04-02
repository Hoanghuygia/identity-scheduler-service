INSERT INTO roles (id, code, name, description, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'ROLE_CUSTOMER', 'Customer', 'Default customer role', NOW()),
    ('22222222-2222-2222-2222-222222222222', 'ROLE_ADMIN', 'Administrator', 'Administrative role', NOW()),
    ('33333333-3333-3333-3333-333333333333', 'ROLE_STAFF', 'Staff', 'Internal staff role', NOW())
ON CONFLICT (code) DO NOTHING;

