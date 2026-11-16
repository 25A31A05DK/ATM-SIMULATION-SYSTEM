-- ============================================================
-- ATM SIMULATION SYSTEM - FREQUENT DATABASE QUERIES
-- Database: atm_db
-- ============================================================

USE atm_db;

-- 1. Authenticate Card and PIN
SELECT a.account_id, a.account_number, a.card_number, a.account_type, a.balance, a.status, u.full_name
FROM accounts a
JOIN users u ON a.user_id = u.user_id
WHERE a.card_number = '1234567890123456' AND a.pin = '1234' AND a.status = 'ACTIVE';

-- 2. Check Account Balance
SELECT account_number, card_number, balance, account_type
FROM accounts
WHERE card_number = '1234567890123456';

-- 3. Withdraw Cash (Update Balance & Record Transaction)
-- (A) Update Balance:
UPDATE accounts
SET balance = balance - 2000.00
WHERE card_number = '1234567890123456' AND balance >= 2000.00;

-- (B) Record Transaction:
INSERT INTO transactions (account_id, transaction_type, amount, balance_after, description)
SELECT account_id, 'WITHDRAWAL', 2000.00, balance, 'ATM Cash Withdrawal'
FROM accounts
WHERE card_number = '1234567890123456';

-- 4. Deposit Cash (Update Balance & Record Transaction)
-- (A) Update Balance:
UPDATE accounts
SET balance = balance + 5000.00
WHERE card_number = '1234567890123456';

-- (B) Record Transaction:
INSERT INTO transactions (account_id, transaction_type, amount, balance_after, description)
SELECT account_id, 'DEPOSIT', 5000.00, balance, 'Cash Deposit via ATM'
FROM accounts
WHERE card_number = '1234567890123456';

-- 5. Change PIN
UPDATE accounts
SET pin = '5678'
WHERE card_number = '1234567890123456' AND pin = '1234';

-- 6. Mini Statement (Latest 5 transactions)
SELECT t.transaction_id, t.transaction_type, t.amount, t.balance_after, t.description, t.transaction_date
FROM transactions t
JOIN accounts a ON t.account_id = a.account_id
WHERE a.card_number = '1234567890123456'
ORDER BY t.transaction_date DESC
LIMIT 5;
