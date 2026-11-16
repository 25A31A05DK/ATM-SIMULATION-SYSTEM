package com.atm.dao;

import com.atm.config.DatabaseConnection;
import com.atm.model.Account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AccountDAO {

    // In-memory fallback repository matching database/sample_data.sql
    private static final Map<String, Account> memoryAccounts = new ConcurrentHashMap<>();

    static {
        // Pre-seed sample cards
        Account acc1 = new Account(1, "Uday Kumar", "ACC100023456", "1234567890123456", "1234",
                "SAVINGS", new BigDecimal("50000.00"), new BigDecimal("40000.00"), "ACTIVE");
        Account acc2 = new Account(2, "Alice Smith", "ACC100098765", "9876543210987654", "4321",
                "SAVINGS", new BigDecimal("25000.00"), new BigDecimal("40000.00"), "ACTIVE");
        Account acc3 = new Account(3, "John Doe", "ACC100011223", "1111222233334444", "9999",
                "CURRENT", new BigDecimal("120000.00"), new BigDecimal("100000.00"), "ACTIVE");

        memoryAccounts.put(acc1.getCardNumber(), acc1);
        memoryAccounts.put(acc2.getCardNumber(), acc2);
        memoryAccounts.put(acc3.getCardNumber(), acc3);
    }

    public Account findByCardNumber(String cardNumber) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            String sql = "SELECT a.*, u.full_name FROM accounts a " +
                         "JOIN users u ON a.user_id = u.user_id " +
                         "WHERE a.card_number = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, cardNumber);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToAccount(rs);
                    }
                }
            } catch (SQLException e) {
                System.err.println("[DAO Error] findByCardNumber: " + e.getMessage());
            } finally {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }

        // Fallback to in-memory store
        return memoryAccounts.get(cardNumber);
    }

    public Account authenticate(String cardNumber, String pin) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            String sql = "SELECT a.*, u.full_name FROM accounts a " +
                         "JOIN users u ON a.user_id = u.user_id " +
                         "WHERE a.card_number = ? AND a.pin = ? AND a.status = 'ACTIVE'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, cardNumber);
                stmt.setString(2, pin);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToAccount(rs);
                    }
                }
            } catch (SQLException e) {
                System.err.println("[DAO Error] authenticate: " + e.getMessage());
            } finally {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }

        // Fallback to in-memory store
        Account acc = memoryAccounts.get(cardNumber);
        if (acc != null && acc.getPin().equals(pin) && "ACTIVE".equalsIgnoreCase(acc.getStatus())) {
            return acc;
        }
        return null;
    }

    public boolean updateBalance(String cardNumber, BigDecimal newBalance) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            String sql = "UPDATE accounts SET balance = ? WHERE card_number = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBigDecimal(1, newBalance);
                stmt.setString(2, cardNumber);
                int rows = stmt.executeUpdate();
                return rows > 0;
            } catch (SQLException e) {
                System.err.println("[DAO Error] updateBalance: " + e.getMessage());
            } finally {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }

        // Fallback to in-memory store
        Account acc = memoryAccounts.get(cardNumber);
        if (acc != null) {
            acc.setBalance(newBalance);
            return true;
        }
        return false;
    }

    public boolean updatePin(String cardNumber, String oldPin, String newPin) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            String sql = "UPDATE accounts SET pin = ? WHERE card_number = ? AND pin = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newPin);
                stmt.setString(2, cardNumber);
                stmt.setString(3, oldPin);
                int rows = stmt.executeUpdate();
                return rows > 0;
            } catch (SQLException e) {
                System.err.println("[DAO Error] updatePin: " + e.getMessage());
            } finally {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }

        // Fallback to in-memory store
        Account acc = memoryAccounts.get(cardNumber);
        if (acc != null && acc.getPin().equals(oldPin)) {
            acc.setPin(newPin);
            return true;
        }
        return false;
    }

    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account acc = new Account();
        acc.setAccountId(rs.getInt("account_id"));
        acc.setUserId(rs.getInt("user_id"));
        acc.setUserName(rs.getString("full_name"));
        acc.setAccountNumber(rs.getString("account_number"));
        acc.setCardNumber(rs.getString("card_number"));
        acc.setPin(rs.getString("pin"));
        acc.setAccountType(rs.getString("account_type"));
        acc.setBalance(rs.getBigDecimal("balance"));
        acc.setDailyWithdrawalLimit(rs.getBigDecimal("daily_withdrawal_limit"));
        acc.setStatus(rs.getString("status"));
        return acc;
    }
}
