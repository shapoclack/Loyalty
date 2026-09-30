-- Стартовые правила; добавляются только в пустые таблицы.

INSERT INTO accrual_rule (name, rule_type, value, category)
SELECT v.name, v.rule_type, v.value, v.category
FROM (VALUES ('Базовый кэшбэк 3%', 'PERCENT', 3, NULL),
             ('Серебряный уровень 5%', 'PERCENT', 5, 'SILVER'),
             ('Золотой уровень 7%', 'PERCENT', 7, 'GOLD'))
     AS v(name, rule_type, value, category)
WHERE NOT EXISTS (SELECT 1 FROM accrual_rule);

INSERT INTO redemption_rule (name, max_percent, min_amount, max_amount, expire_days)
SELECT 'Оплата до 50% чека', 50, 1, NULL, 3
WHERE NOT EXISTS (SELECT 1 FROM redemption_rule);
