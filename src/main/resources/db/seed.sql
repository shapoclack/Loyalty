-- Начальные данные. Каждый блок выполняется только на пустых таблицах,
-- поэтому повторный запуск программы ничего не дублирует.
-- Балансы счетов сведены с bonus_transaction, уровни — с суммой покупок клиента.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM accrual_rule) THEN
        INSERT INTO accrual_rule (rule_id, name, rule_type, value, category, valid_from, valid_to, is_active) VALUES
            (1, 'Базовый кэшбэк 3%',        'PERCENT', 3, NULL,     '2026-01-01', NULL,         TRUE),
            (2, 'Серебряный уровень 5%',    'PERCENT', 5, 'SILVER', '2026-01-01', NULL,         TRUE),
            (3, 'Золотой уровень 7%',       'PERCENT', 7, 'GOLD',   '2026-01-01', NULL,         TRUE),
            (4, 'Новогодняя акция: 5 за каждые 100 руб.', 'PER_100', 5, NULL, '2026-12-25', '2027-01-09', TRUE),
            (5, 'Приветственный бонус 100', 'FIXED',   100, NULL,   '2026-01-01', NULL,         FALSE);
        PERFORM setval(pg_get_serial_sequence('accrual_rule', 'rule_id'), (SELECT max(rule_id) FROM accrual_rule));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM redemption_rule) THEN
        INSERT INTO redemption_rule (rule_id, name, max_percent, min_amount, max_amount, expire_days, is_active) VALUES
            (1, 'Оплата до 50% чека',       50,  1, NULL, 3, TRUE),
            (2, 'Распродажа: до 100% чека', 100, 1, 1000, 1, FALSE);
        PERFORM setval(pg_get_serial_sequence('redemption_rule', 'rule_id'), (SELECT max(rule_id) FROM redemption_rule));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM customer) THEN
        INSERT INTO customer (customer_id, full_name, phone, email, birth_date, created_at, status) VALUES
            (1, 'Иванов Иван Петрович',     '+79001110001', 'ivanov@mail.ru',       '1990-05-15', '2026-01-10 10:00', 'ACTIVE'),
            (2, 'Петрова Анна Сергеевна',   '+79001110002', 'petrova@mail.ru',      '1985-11-02', '2026-01-15 11:30', 'ACTIVE'),
            (3, 'Сидоров Алексей Игоревич', '+79001110003', 'sidorov@yandex.ru',    '1978-03-21', '2026-02-01 09:15', 'ACTIVE'),
            (4, 'Кузнецова Мария Олеговна', '+79001110004', 'kuznetsova@gmail.com', '2001-07-30', '2026-03-05 18:40', 'ACTIVE'),
            (5, 'Смирнов Дмитрий Андреевич','+79001110005', NULL,                   '1995-12-12', '2026-03-20 14:05', 'BLOCKED'),
            (6, 'Волкова Елена Викторовна', '+79001110006', 'volkova@mail.ru',      NULL,         '2026-06-01 12:00', 'ACTIVE');
        PERFORM setval(pg_get_serial_sequence('customer', 'customer_id'), (SELECT max(customer_id) FROM customer));

        INSERT INTO loyalty_account (account_id, customer_id, card_number, balance, tier, status, opened_at, updated_at) VALUES
            (1, 1, '7700000000000001', 445.00, 'SILVER', 'ACTIVE',  '2026-01-10 10:00', '2026-05-18 16:20'),
            (2, 2, '7700000000000002', 780.00, 'GOLD',   'ACTIVE',  '2026-01-15 11:30', '2026-09-25 19:10'),
            (3, 3, '7700000000000003', 150.00, 'SILVER', 'ACTIVE',  '2026-02-01 09:15', '2026-06-22 13:45'),
            (4, 4, '7700000000000004', 111.00, 'BASE',   'ACTIVE',  '2026-03-05 18:40', '2026-07-05 00:00'),
            (5, 5, '7700000000000005',  96.00, 'BASE',   'BLOCKED', '2026-03-20 14:05', '2026-08-12 17:30'),
            (6, 6, '7700000000000006', 195.00, 'BASE',   'ACTIVE',  '2026-06-01 12:00', '2026-09-15 11:25'),
            (7, 1, '7700000000000007',   0.00, 'BASE',   'CLOSED',  '2026-02-20 15:00', '2026-04-01 10:00');
        PERFORM setval(pg_get_serial_sequence('loyalty_account', 'account_id'), (SELECT max(account_id) FROM loyalty_account));

        INSERT INTO source_operation (operation_id, customer_id, operation_type, external_id, amount, operation_date, store_code) VALUES
            (1,  2, 'PURCHASE', 'CHK-0001', 30000.00, '2026-01-20 12:30', 'SHOP-01'),
            (2,  2, 'PURCHASE', 'CHK-0002', 25000.00, '2026-02-14 17:45', 'SHOP-02'),
            (3,  2, 'PURCHASE', 'CHK-0003',  8000.00, '2026-04-10 10:05', 'SHOP-01'),
            (4,  2, 'PURCHASE', 'CHK-0004',  3000.00, '2026-09-25 19:10', 'SHOP-03'),
            (5,  1, 'PURCHASE', 'CHK-0005',  4500.00, '2026-01-25 13:00', 'SHOP-01'),
            (6,  1, 'PURCHASE', 'CHK-0006',  7000.00, '2026-03-03 18:20', 'SHOP-02'),
            (7,  1, 'PURCHASE', 'CHK-0007',  2000.00, '2026-05-18 16:20', 'SHOP-02'),
            (8,  3, 'PURCHASE', 'CHK-0008', 12500.00, '2026-02-10 11:10', 'SHOP-03'),
            (9,  3, 'PURCHASE', 'CHK-0009',  1800.00, '2026-06-22 13:45', 'SHOP-01'),
            (10, 4, 'PURCHASE', 'CHK-0010',  2500.00, '2026-03-15 20:00', 'SHOP-02'),
            (11, 4, 'PURCHASE', 'CHK-0011',  1200.00, '2026-07-01 09:30', 'SHOP-03'),
            (12, 5, 'PURCHASE', 'CHK-0012',  3200.00, '2026-03-25 15:15', 'SHOP-01'),
            (13, 5, 'PURCHASE', 'CHK-0013',  1500.00, '2026-08-12 17:30', 'SHOP-01'),
            (14, 6, 'PURCHASE', 'CHK-0014',   900.00, '2026-06-05 10:40', 'SHOP-03'),
            (15, 6, 'PURCHASE', 'CHK-0015',  5600.00, '2026-09-15 11:25', 'SHOP-02');
        PERFORM setval(pg_get_serial_sequence('source_operation', 'operation_id'), (SELECT max(operation_id) FROM source_operation));

        -- Начисление считается от суммы, оплаченной деньгами (сумма операции минус списанные бонусы),
        -- по ставке уровня, который был у счёта на момент покупки.
        INSERT INTO accrual_request (request_id, account_id, customer_id, rule_id, operation_id, bonus_amount, status, created_at, processed_at) VALUES
            (1,  2, 2, 1,    1,   900.00, 'APPROVED', '2026-01-20 12:30', '2026-01-20 12:30'),
            (2,  2, 2, 2,    2,  1250.00, 'APPROVED', '2026-02-14 17:45', '2026-02-14 17:45'),
            (3,  2, 2, 3,    3,   420.00, 'APPROVED', '2026-04-10 10:05', '2026-04-10 10:05'),
            (4,  2, 2, 3,    4,   210.00, 'APPROVED', '2026-09-25 19:10', '2026-09-25 19:10'),
            (5,  1, 1, 1,    5,   135.00, 'APPROVED', '2026-01-25 13:00', '2026-01-25 13:00'),
            (6,  1, 1, 1,    6,   210.00, 'APPROVED', '2026-03-03 18:20', '2026-03-03 18:20'),
            (7,  1, 1, 2,    7,   100.00, 'APPROVED', '2026-05-18 16:20', '2026-05-18 16:20'),
            (8,  3, 3, 1,    8,   375.00, 'APPROVED', '2026-02-10 11:10', '2026-02-10 11:10'),
            (9,  3, 3, 2,    9,    75.00, 'APPROVED', '2026-06-22 13:45', '2026-06-22 13:45'),
            (10, 4, 4, 1,    10,   75.00, 'APPROVED', '2026-03-15 20:00', '2026-03-15 20:00'),
            (11, 4, 4, 1,    11,   36.00, 'APPROVED', '2026-07-01 09:30', '2026-07-01 09:30'),
            (12, 5, 5, 1,    12,   96.00, 'APPROVED', '2026-03-25 15:15', '2026-03-25 15:15'),
            (13, 5, 5, NULL, 13,    0.00, 'REJECTED', '2026-08-12 17:30', '2026-08-12 17:30'),
            (14, 6, 6, 1,    14,   27.00, 'APPROVED', '2026-06-05 10:40', '2026-06-05 10:40'),
            (15, 6, 6, 1,    15,  168.00, 'APPROVED', '2026-09-15 11:25', '2026-09-15 11:25');
        PERFORM setval(pg_get_serial_sequence('accrual_request', 'request_id'), (SELECT max(request_id) FROM accrual_request));

        INSERT INTO redemption_request (request_id, account_id, customer_id, rule_id, operation_id, bonus_amount, status, created_at, processed_at) VALUES
            (1, 2, 2, 1, 3,  2000.00, 'APPROVED',  '2026-04-10 10:05', '2026-04-10 10:05'),
            (2, 1, 1, 1, 7,   300.00, 'CANCELLED', '2026-05-18 16:15', '2026-05-18 16:18'),
            (3, 3, 3, 1, 9,   300.00, 'APPROVED',  '2026-06-22 13:45', '2026-06-22 13:45'),
            (4, 4, 4, 1, 11,   50.00, 'EXPIRED',   '2026-07-01 09:30', '2026-07-05 00:00'),
            (5, 2, 2, 1, 4,   500.00, 'PENDING',   '2026-09-29 12:00', NULL);
        PERFORM setval(pg_get_serial_sequence('redemption_request', 'request_id'), (SELECT max(request_id) FROM redemption_request));

        INSERT INTO bonus_transaction (transaction_id, account_id, accrual_request_id, redemption_request_id, transaction_type, amount, balance_after, created_at) VALUES
            (1,  2, 1,    NULL, 'ACCRUAL',     900.00,  900.00, '2026-01-20 12:30'),
            (2,  1, 5,    NULL, 'ACCRUAL',     135.00,  135.00, '2026-01-25 13:00'),
            (3,  3, 8,    NULL, 'ACCRUAL',     375.00,  375.00, '2026-02-10 11:10'),
            (4,  2, 2,    NULL, 'ACCRUAL',    1250.00, 2150.00, '2026-02-14 17:45'),
            (5,  1, 6,    NULL, 'ACCRUAL',     210.00,  345.00, '2026-03-03 18:20'),
            (6,  4, 10,   NULL, 'ACCRUAL',      75.00,   75.00, '2026-03-15 20:00'),
            (7,  5, 12,   NULL, 'ACCRUAL',      96.00,   96.00, '2026-03-25 15:15'),
            (8,  2, NULL, 1,    'REDEMPTION', -2000.00,  150.00, '2026-04-10 10:05'),
            (9,  2, 3,    NULL, 'ACCRUAL',     420.00,  570.00, '2026-04-10 10:05'),
            (10, 1, 7,    NULL, 'ACCRUAL',     100.00,  445.00, '2026-05-18 16:20'),
            (11, 6, 14,   NULL, 'ACCRUAL',      27.00,   27.00, '2026-06-05 10:40'),
            (12, 3, NULL, 3,    'REDEMPTION',  -300.00,   75.00, '2026-06-22 13:45'),
            (13, 3, 9,    NULL, 'ACCRUAL',      75.00,  150.00, '2026-06-22 13:45'),
            (14, 4, 11,   NULL, 'ACCRUAL',      36.00,  111.00, '2026-07-01 09:30'),
            (15, 6, 15,   NULL, 'ACCRUAL',     168.00,  195.00, '2026-09-15 11:25'),
            (16, 2, 4,    NULL, 'ACCRUAL',     210.00,  780.00, '2026-09-25 19:10');
        PERFORM setval(pg_get_serial_sequence('bonus_transaction', 'transaction_id'), (SELECT max(transaction_id) FROM bonus_transaction));
    END IF;
END $$;
