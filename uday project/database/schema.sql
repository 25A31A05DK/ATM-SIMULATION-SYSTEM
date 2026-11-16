-- ============================================================
-- ATM SIMULATION SYSTEM - DATABASE SCHEMA
-- Database: atm_db
-- Compatible with: MySQL 5.7+ / 8.0+ / MariaDB / PostgreSQL
-- ============================================================

CREATE DATABASE IF NOT EXISTS atm_db;
USE atm_db;

-- 1. Table: users
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Table: accounts
CREATE TABLE accounts (
    account_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    card_number VARCHAR(16) UNIQUE NOT NULL,
    pin VARCHAR(10) NOT NULL, -- Stored as 4-digit PIN (or hashed string)
    account_type ENUM('SAVINGS', 'CURRENT') DEFAULT 'SAVINGS',
    balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    daily_withdrawal_limit DECIMAL(10, 2) DEFAULT 40000.00,
    status ENUM('ACTIVE', 'BLOCKED', 'CLOSED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_account FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Table: transactions
CREATE TABLE transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    transaction_type ENUM('DEPOSIT', 'WITHDRAWAL', 'PIN_CHANGE', 'TRANSFER') NOT NULL,
    amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    balance_after DECIMAL(12, 2) NOT NULL,
    description VARCHAR(255),
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_transaction FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Indexes for fast query lookup at ATM terminals
CREATE INDEX idx_card_number ON accounts(card_number);
CREATE INDEX idx_account_number ON accounts(account_number);
CREATE INDEX idx_account_trans ON transactions(account_id, transaction_date DESC);
