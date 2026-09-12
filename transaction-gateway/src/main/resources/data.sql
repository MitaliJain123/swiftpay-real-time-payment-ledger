-- Seed users for local development / demo.
-- Runs after Hibernate creates the schema; ON CONFLICT keeps it idempotent
-- so existing balances are never overwritten on restart.

INSERT INTO users (id, name, balance, currency, created_at)
VALUES
    (1, 'Alice',   10000.00, 'INR', NOW()),
    (2, 'Bob',      5000.00, 'INR', NOW()),
    (3, 'Charlie',   750.00, 'INR', NOW())
ON CONFLICT (id) DO NOTHING;
