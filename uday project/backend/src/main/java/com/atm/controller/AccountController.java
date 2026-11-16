package com.atm.controller;

import com.atm.config.CorsFilter;
import com.atm.model.Account;
import com.atm.model.Transaction;
import com.atm.service.AtmService;
import com.atm.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class AccountController implements HttpHandler {

    private final AtmService atmService = new AtmService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (CorsFilter.handlePreflight(exchange)) {
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (!"POST".equalsIgnoreCase(method)) {
            CorsFilter.sendJsonResponse(exchange, 405, JsonHelper.errorResponse("Method Not Allowed. Use POST."));
            return;
        }

        String body = readRequestBody(exchange);
        Map<String, String> data = JsonHelper.parseSimpleJson(body);

        String cardNumber = data.get("cardNumber");
        String pin = data.get("pin");

        try {
            switch (path) {
                case "/api/auth/login":
                    handleLogin(exchange, cardNumber, pin);
                    break;

                case "/api/atm/balance":
                    handleBalance(exchange, cardNumber, pin);
                    break;

                case "/api/atm/withdraw":
                    handleWithdraw(exchange, cardNumber, pin, data.get("amount"));
                    break;

                case "/api/atm/deposit":
                    handleDeposit(exchange, cardNumber, pin, data.get("amount"));
                    break;

                case "/api/atm/change-pin":
                    handleChangePin(exchange, cardNumber, pin, data.get("newPin"));
                    break;

                case "/api/atm/statement":
                    handleStatement(exchange, cardNumber, pin);
                    break;

                default:
                    CorsFilter.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("Endpoint not found: " + path));
                    break;
            }
        } catch (Exception e) {
            CorsFilter.sendJsonResponse(exchange, 400, JsonHelper.errorResponse(e.getMessage()));
        }
    }

    private void handleLogin(HttpExchange exchange, String cardNumber, String pin) throws IOException {
        try {
            Account account = atmService.login(cardNumber, pin);
            String extra = String.format(
                    "\"account\":{\"userName\":\"%s\",\"accountNumber\":\"%s\",\"accountType\":\"%s\",\"balance\":%.2f}",
                    JsonHelper.escape(account.getUserName()),
                    JsonHelper.escape(account.getAccountNumber()),
                    JsonHelper.escape(account.getAccountType()),
                    account.getBalance().doubleValue()
            );
            CorsFilter.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Login successful", extra));
        } catch (Exception e) {
            CorsFilter.sendJsonResponse(exchange, 401, JsonHelper.errorResponse(e.getMessage()));
        }
    }

    private void handleBalance(HttpExchange exchange, String cardNumber, String pin) throws IOException {
        try {
            BigDecimal balance = atmService.getBalance(cardNumber, pin);
            String extra = String.format("\"balance\":%.2f", balance.doubleValue());
            CorsFilter.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Balance retrieved", extra));
        } catch (Exception e) {
            CorsFilter.sendJsonResponse(exchange, 400, JsonHelper.errorResponse(e.getMessage()));
        }
    }

    private void handleWithdraw(HttpExchange exchange, String cardNumber, String pin, String amountStr) throws IOException {
        try {
            if (amountStr == null || amountStr.trim().isEmpty()) {
                throw new Exception("Amount is required.");
            }
            BigDecimal amount = new BigDecimal(amountStr.trim());
            Account account = atmService.withdraw(cardNumber, pin, amount);
            String extra = String.format("\"withdrawn\":%.2f,\"balance\":%.2f", amount.doubleValue(), account.getBalance().doubleValue());
            CorsFilter.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Please collect your cash: ₹" + amount, extra));
        } catch (NumberFormatException e) {
            CorsFilter.sendJsonResponse(exchange, 400, JsonHelper.errorResponse("Invalid amount format."));
        } catch (Exception e) {
            CorsFilter.sendJsonResponse(exchange, 400, JsonHelper.errorResponse(e.getMessage()));
        }
    }

    private void handleDeposit(HttpExchange exchange, String cardNumber, String pin, String amountStr) throws IOException {
        try {
            if (amountStr == null || amountStr.trim().isEmpty()) {
                throw new Exception("Amount is required.");
            }
            BigDecimal amount = new BigDecimal(amountStr.trim());
            Account account = atmService.deposit(cardNumber, pin, amount);
            String extra = String.format("\"deposited\":%.2f,\"balance\":%.2f", amount.doubleValue(), account.getBalance().doubleValue());
            CorsFilter.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Amount ₹" + amount + " deposited successfully!", extra));
        } catch (NumberFormatException e) {
            CorsFilter.sendJsonResponse(exchange, 400, JsonHelper.errorResponse("Invalid amount format."));
        } catch (Exception e) {
            CorsFilter.sendJsonResponse(exchange, 400, JsonHelper.errorResponse(e.getMessage()));
        }
    }

    private void handleChangePin(HttpExchange exchange, String cardNumber, String oldPin, String newPin) throws IOException {
        try {
            atmService.changePin(cardNumber, oldPin, newPin);
            CorsFilter.sendJsonResponse(exchange, 200, JsonHelper.successResponse("PIN changed successfully! Please login with your new PIN.", null));
        } catch (Exception e) {
            CorsFilter.sendJsonResponse(exchange, 400, JsonHelper.errorResponse(e.getMessage()));
        }
    }

    private void handleStatement(HttpExchange exchange, String cardNumber, String pin) throws IOException {
        try {
            List<Transaction> list = atmService.getMiniStatement(cardNumber, pin);
            StringBuilder sb = new StringBuilder();
            sb.append("\"transactions\":[");
            for (int i = 0; i < list.size(); i++) {
                Transaction t = list.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format(
                        "{\"id\":%d,\"type\":\"%s\",\"amount\":%.2f,\"balanceAfter\":%.2f,\"description\":\"%s\",\"date\":\"%s\"}",
                        t.getTransactionId(),
                        t.getTransactionType(),
                        t.getAmount().doubleValue(),
                        t.getBalanceAfter().doubleValue(),
                        JsonHelper.escape(t.getDescription()),
                        JsonHelper.escape(t.getTransactionDate())
                ));
            }
            sb.append("]");
            CorsFilter.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Mini statement fetched", sb.toString()));
        } catch (Exception e) {
            CorsFilter.sendJsonResponse(exchange, 400, JsonHelper.errorResponse(e.getMessage()));
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
