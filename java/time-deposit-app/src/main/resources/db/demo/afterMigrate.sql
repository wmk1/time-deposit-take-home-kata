-- Demo data as a Flyway afterMigrate callback, not a versioned migration, so a future schema V2 cannot
-- clash with it. The callback runs after every migrate and inserts only the rows that do not exist yet.
-- Pairs cover the day immediately before and after an interest eligibility change.
INSERT INTO "timeDeposits" (id, "planType", days, balance)
VALUES (1, 'basic', 30, 1200.00),
       (2, 'basic', 31, 1200.00),
       (3, 'student', 30, 1200.00),
       (4, 'student', 31, 1200.00),
       (5, 'student', 365, 1200.00),
       (6, 'student', 366, 1200.00),
       (7, 'premium', 45, 1200.00),
       (8, 'premium', 46, 1200.00),
       (9, 'unknown', 46, 1200.00)
ON CONFLICT (id) DO NOTHING;

INSERT INTO withdrawals (id, "timeDepositId", amount, date)
VALUES (1, 2, 25.50, DATE '2024-05-01'),
       (2, 2, 10.00, DATE '2024-05-15'),
       (3, 8, 45.75, DATE '2024-05-24')
ON CONFLICT (id) DO NOTHING;
