package com.atm.dao;

import com.atm.config.DatabaseConnection;
import com.atm.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionDAO {

    private static final List<Transaction> memoryTransactions = new CopyOnWriteArrayList<>();
    private static final AtomicInteger transIdGenerator = new AtomicInteger(100);

    static {
        // Pre-seed some sample transactions for Card 1 (account_id = 1)
        memoryTransactions.add(new Transaction(1, 1, "DEPOSIT", new BigDecimal("50000.00"), new BigDecimal("50000.00"), "Initial Deposit", "2026-11-06 10:30:00"));
        memoryTransactions.add(new Transaction(2, 1, "WITHDRAWAL", new BigDecimal("5000.00"), new BigDecimal("45000.00"), "ATM Cash Withdrawal", "2026-11-11 14:15:00"));
        memoryTransactions.add(new Transaction(3, 1, "DEPOSIT", new BigDecimal("10000.00"), new BigDecimal("55000.00"), "Cash Deposit via ATM", "2026-11-14 18:00:00"));
        memoryTransactions.add(new Transaction(4, 1, "WITHDRAWAL", new BigDecimal("5000.00"), new BigDecimal("50000.00"), "ATM Cash Withdrawal", "2026-11-15 09:20:00"));
    }

    public boolean recordTransaction(int accountId, String type, BigDecimal amount, BigDecimal balanceAfter, String description) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            String sql = "INSERT INTO transactions (account_id, transaction_type, amount, balance_after, description) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, accountId);
                stmt.setString(2, type);
                stmt.setBigDecimal(3, amount);
                stmt.setBigDecimal(4, balanceAfter);
                stmt.setString(5, description);
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[DAO Error] recordTransaction: " + e.getMessage());
            } finally {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }

        // Fallback to in-memory store
        Transaction tx = new Transaction(
                transIdGenerator.incrementAndGet(),
                accountId,
                type,
                amount,
                balanceAfter,
                description,
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
        );
        memoryTransactions.add(0, tx); // newest first
        return true;
    }

    public List<Transaction> getRecentTransactions(int accountId, int limit) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            List<Transaction> list = new ArrayList<>();
            String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY transaction_date DESC LIMIT ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, accountId);
                stmt.setInt(2, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Transaction tx = new Transaction();
                        tx.setTransactionId(rs.getInt("transaction_id"));
                        tx.setAccountId(rs.getInt("account_id"));
                        tx.setTransactionType(rs.getString("transaction_type"));
                        tx.setAmount(rs.getBigDecimal("amount"));
                        tx.setBalanceAfter(rs.getBigDecimal("balance_after"));
                        tx.setDescription(rs.getString("description"));
                        tx.setTransactionDate(rs.getTimestamp("transaction_date").toString());
                        list.add(tx);
                    }
                    return list;
                }
            } catch (SQLException e) {
                System.err.println("[DAO Error] getRecentTransactions: " + e.getMessage());
            } finally {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }

        // Fallback to in-memory store
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction tx : memoryTransactions) {
            if (tx.getAccountId() == accountId) {
                filtered.add(tx);
                if (filtered.size() >= limit) break;
            }
        }
        return filtered;
    }
}
