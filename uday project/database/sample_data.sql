-- ============================================================
-- ATM SIMULATION SYSTEM - SAMPLE SEED DATA
-- Database: atm_db
-- ============================================================

USE atm_db;

-- Insert Sample Users
INSERT INTO users (user_id, full_name, email, phone, address) VALUES
(1, 'Uday Kumar', 'uday@example.com', '9876543210', 'Plot 42, Hitech City, Hyderabad'),
(2, 'Alice Smith', 'alice.smith@example.com', '9123456780', 'Flat 304, Green Park, Bangalore'),
(3, 'John Doe', 'john.doe@example.com', '9988776655', 'Sector 15, New Delhi');

-- Insert Sample Accounts (Linked to Cards & PINs)
-- Card 1: 1234567890123456, PIN: 1234, Initial Balance: 50,000.00
-- Card 2: 9876543210987654, PIN: 4321, Initial Balance: 25,000.00
-- Card 3: 1111222233334444, PIN: 9999, Initial Balance: 120,000.00
INSERT INTO accounts (account_id, user_id, account_number, card_number, pin, account_type, balance, daily_withdrawal_limit, status) VALUES
(1, 1, 'ACC100023456', '1234567890123456', '1234', 'SAVINGS', 50000.00, 40000.00, 'ACTIVE'),
(2, 2, 'ACC100098765', '9876543210987654', '4321', 'SAVINGS', 25000.00, 40000.00, 'ACTIVE'),
(3, 3, 'ACC100011223', '1111222233334444', '9999', 'CURRENT', 120000.00, 100000.00, 'ACTIVE');

-- Insert Sample Transactions for Card 1 (Uday Kumar)
INSERT INTO transactions (account_id, transaction_type, amount, balance_after, description, transaction_date) VALUES
(1, 'DEPOSIT', 50000.00, 50000.00, 'Initial Account Deposit', NOW() - INTERVAL 10 DAY),
(1, 'WITHDRAWAL', 5000.00, 45000.00, 'ATM Cash Withdrawal', NOW() - INTERVAL 5 DAY),
(1, 'DEPOSIT', 10000.00, 55000.00, 'Cash Deposit via ATM Machine', NOW() - INTERVAL 2 DAY),
(1, 'WITHDRAWAL', 5000.00, 50000.00, 'ATM Cash Withdrawal', NOW() - INTERVAL 1 DAY);

-- Insert Sample Transactions for Card 2 (Alice Smith)
INSERT INTO transactions (account_id, transaction_type, amount, balance_after, description, transaction_date) VALUES
(2, 'DEPOSIT', 30000.00, 30000.00, 'Account Opening Deposit', NOW() - INTERVAL 15 DAY),
(2, 'WITHDRAWAL', 5000.00, 25000.00, 'ATM Cash Withdrawal', NOW() - INTERVAL 3 DAY);
