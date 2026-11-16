package com.atm.service;

import com.atm.dao.AccountDAO;
import com.atm.dao.TransactionDAO;
import com.atm.model.Account;
import com.atm.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

public class AtmService {

    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;

    public AtmService() {
        this.accountDAO = new AccountDAO();
        this.transactionDAO = new TransactionDAO();
    }

    public Account login(String cardNumber, String pin) throws Exception {
        if (cardNumber == null || cardNumber.trim().length() < 12) {
            throw new Exception("Invalid card number format. Must be at least 12-16 digits.");
        }
        if (pin == null || pin.trim().length() != 4) {
            throw new Exception("PIN must be exactly 4 digits.");
        }

        Account account = accountDAO.authenticate(cardNumber.trim(), pin.trim());
        if (account == null) {
            throw new Exception("Authentication failed. Incorrect Card Number or PIN.");
        }
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new Exception("Account is currently " + account.getStatus() + ". Please contact your bank.");
        }
        return account;
    }

    public BigDecimal getBalance(String cardNumber, String pin) throws Exception {
        Account account = login(cardNumber, pin);
        return account.getBalance();
    }

    public synchronized Account withdraw(String cardNumber, String pin, BigDecimal amount) throws Exception {
        Account account = login(cardNumber, pin);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Withdrawal amount must be greater than zero.");
        }

        // ATM denomination check (multiples of 100)
        if (amount.remainder(new BigDecimal("100")).compareTo(BigDecimal.ZERO) != 0) {
            throw new Exception("Withdrawal amount must be a multiple of 100 (e.g. 100, 500, 2000).");
        }

        if (amount.compareTo(account.getDailyWithdrawalLimit()) > 0) {
            throw new Exception("Amount exceeds daily withdrawal limit of ₹" + account.getDailyWithdrawalLimit());
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new Exception("Insufficient balance! Current balance: ₹" + account.getBalance());
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        boolean updated = accountDAO.updateBalance(cardNumber, newBalance);
        if (!updated) {
            throw new Exception("Transaction failed while updating account balance.");
        }

        account.setBalance(newBalance);
        transactionDAO.recordTransaction(
                account.getAccountId(),
                "WITHDRAWAL",
                amount,
                newBalance,
                "ATM Cash Withdrawal"
        );

        return account;
    }

    public synchronized Account deposit(String cardNumber, String pin, BigDecimal amount) throws Exception {
        Account account = login(cardNumber, pin);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Deposit amount must be greater than zero.");
        }

        if (amount.remainder(new BigDecimal("100")).compareTo(BigDecimal.ZERO) != 0) {
            throw new Exception("Deposit notes must be in denominations of 100, 200, or 500.");
        }

        if (amount.compareTo(new BigDecimal("200000.00")) > 0) {
            throw new Exception("Single transaction deposit cannot exceed ₹200,000.");
        }

        BigDecimal newBalance = account.getBalance().add(amount);
        boolean updated = accountDAO.updateBalance(cardNumber, newBalance);
        if (!updated) {
            throw new Exception("Transaction failed while updating account balance.");
        }

        account.setBalance(newBalance);
        transactionDAO.recordTransaction(
                account.getAccountId(),
                "DEPOSIT",
                amount,
                newBalance,
                "Cash Deposit via ATM"
        );

        return account;
    }

    public boolean changePin(String cardNumber, String oldPin, String newPin) throws Exception {
        login(cardNumber, oldPin); // verify existing credentials

        if (newPin == null || newPin.trim().length() != 4 || !newPin.matches("\\d{4}")) {
            throw new Exception("New PIN must be a 4-digit number.");
        }

        if (newPin.equals(oldPin)) {
            throw new Exception("New PIN cannot be the same as your old PIN.");
        }

        boolean updated = accountDAO.updatePin(cardNumber, oldPin, newPin);
        if (!updated) {
            throw new Exception("Failed to update PIN. Please try again.");
        }

        Account acc = accountDAO.findByCardNumber(cardNumber);
        if (acc != null) {
            transactionDAO.recordTransaction(
                    acc.getAccountId(),
                    "PIN_CHANGE",
                    BigDecimal.ZERO,
                    acc.getBalance(),
                    "ATM PIN Change Successful"
            );
        }

        return true;
    }

    public List<Transaction> getMiniStatement(String cardNumber, String pin) throws Exception {
        Account account = login(cardNumber, pin);
        return transactionDAO.getRecentTransactions(account.getAccountId(), 10);
    }
}
