package com.atm.server;

import com.atm.model.Account;
import com.atm.model.Transaction;
import com.atm.service.AtmService;
import com.atm.util.JsonHelper;

import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Robust, zero-dependency HTTP 1.1 Server built directly on ServerSocket.
 * Runs on any standard JVM without requiring heavy containers (Tomcat) or internal JDK modules.
 */
public class AtmHttpServer {

    private final int port;
    private final AtmService atmService;
    private final Path frontendPath;
    private final ExecutorService threadPool;
    private volatile boolean running = false;
    private ServerSocket serverSocket;

    public AtmHttpServer(int port) {
        this.port = port;
        this.atmService = new AtmService();
        this.threadPool = Executors.newFixedThreadPool(15);

        // Resolve frontend folder location
        Path path = Paths.get("frontend").toAbsolutePath().normalize();
        if (!Files.exists(path) && Files.exists(Paths.get("../frontend"))) {
            path = Paths.get("../frontend").toAbsolutePath().normalize();
        }
        this.frontendPath = path;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        System.out.println("==================================================================");
        System.out.println("       ATM SIMULATION SYSTEM - BACKEND SERVER STARTED             ");
        System.out.println("==================================================================");
        System.out.println(" >> Web Application URL:   http://localhost:" + port + "/");
        System.out.println(" >> API Base URL:          http://localhost:" + port + "/api/");
        System.out.println(" >> Serving Frontend from: " + frontendPath.toString());
        System.out.println("------------------------------------------------------------------");
        System.out.println(" Available Demo Cards for Testing:");
        System.out.println("   Card: 1234567890123456 | PIN: 1234 | Name: Uday Kumar  | Bal: Rs 50,000");
        System.out.println("   Card: 9876543210987654 | PIN: 4321 | Name: Alice Smith | Bal: Rs 25,000");
        System.out.println("   Card: 1111222233334444 | PIN: 9999 | Name: John Doe    | Bal: Rs 120,000");
        System.out.println("------------------------------------------------------------------");
        System.out.println(" Server is running smoothly! Press Ctrl+C in this console to stop.");
        System.out.println("==================================================================");

        threadPool.submit(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (!running) break;
                }
            }
        });
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        threadPool.shutdown();
    }

    private void handleClient(Socket socket) {
        try (socket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            // Read request line and headers
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.trim().isEmpty()) {
                return;
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;

            String method = parts[0].toUpperCase();
            String rawUri = parts[1];
            String path = rawUri.contains("?") ? rawUri.substring(0, rawUri.indexOf('?')) : rawUri;

            // Read headers
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            int contentLength = 0;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                int colon = headerLine.indexOf(':');
                if (colon > 0) {
                    String name = headerLine.substring(0, colon).trim().toLowerCase();
                    String val = headerLine.substring(colon + 1).trim();
                    headers.put(name, val);
                    if ("content-length".equals(name)) {
                        try { contentLength = Integer.parseInt(val); } catch (Exception ignored) {}
                    }
                }
            }

            // Handle CORS preflight
            if ("OPTIONS".equals(method)) {
                sendCorsPreflightResponse(out);
                return;
            }

            // Handle API routes
            if (path.startsWith("/api/")) {
                char[] bodyChars = new char[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int r = reader.read(bodyChars, read, contentLength - read);
                    if (r == -1) break;
                    read += r;
                }
                String body = new String(bodyChars);
                handleApiRequest(out, method, path, body);
                return;
            }

            // Handle Static Frontend Files
            handleStaticFile(out, path);

        } catch (Exception e) {
            // Client disconnect or socket reset
        }
    }

    private void handleApiRequest(OutputStream out, String method, String path, String body) throws IOException {
        if (!"POST".equalsIgnoreCase(method)) {
            sendJsonResponse(out, 405, JsonHelper.errorResponse("Method Not Allowed. Use POST."));
            return;
        }

        Map<String, String> data = JsonHelper.parseSimpleJson(body);
        String cardNumber = data.get("cardNumber");
        String pin = data.get("pin");

        try {
            switch (path) {
                case "/api/auth/login": {
                    Account account = atmService.login(cardNumber, pin);
                    String extra = String.format(
                            "\"account\":{\"userName\":\"%s\",\"accountNumber\":\"%s\",\"accountType\":\"%s\",\"balance\":%.2f}",
                            JsonHelper.escape(account.getUserName()),
                            JsonHelper.escape(account.getAccountNumber()),
                            JsonHelper.escape(account.getAccountType()),
                            account.getBalance().doubleValue()
                    );
                    sendJsonResponse(out, 200, JsonHelper.successResponse("Login successful", extra));
                    break;
                }

                case "/api/atm/balance": {
                    BigDecimal balance = atmService.getBalance(cardNumber, pin);
                    String extra = String.format("\"balance\":%.2f", balance.doubleValue());
                    sendJsonResponse(out, 200, JsonHelper.successResponse("Balance retrieved", extra));
                    break;
                }

                case "/api/atm/withdraw": {
                    String amountStr = data.get("amount");
                    if (amountStr == null || amountStr.trim().isEmpty()) {
                        throw new Exception("Amount is required.");
                    }
                    BigDecimal amount = new BigDecimal(amountStr.trim());
                    Account account = atmService.withdraw(cardNumber, pin, amount);
                    String extra = String.format("\"withdrawn\":%.2f,\"balance\":%.2f", amount.doubleValue(), account.getBalance().doubleValue());
                    sendJsonResponse(out, 200, JsonHelper.successResponse("Cash withdrawn successfully", extra));
                    break;
                }

                case "/api/atm/deposit": {
                    String amountStr = data.get("amount");
                    if (amountStr == null || amountStr.trim().isEmpty()) {
                        throw new Exception("Amount is required.");
                    }
                    BigDecimal amount = new BigDecimal(amountStr.trim());
                    Account account = atmService.deposit(cardNumber, pin, amount);
                    String extra = String.format("\"deposited\":%.2f,\"balance\":%.2f", amount.doubleValue(), account.getBalance().doubleValue());
                    sendJsonResponse(out, 200, JsonHelper.successResponse("Cash deposited successfully", extra));
                    break;
                }

                case "/api/atm/change-pin": {
                    String newPin = data.get("newPin");
                    atmService.changePin(cardNumber, pin, newPin);
                    sendJsonResponse(out, 200, JsonHelper.successResponse("PIN changed successfully!", null));
                    break;
                }

                case "/api/atm/statement": {
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
                    sendJsonResponse(out, 200, JsonHelper.successResponse("Mini statement fetched", sb.toString()));
                    break;
                }

                default:
                    sendJsonResponse(out, 404, JsonHelper.errorResponse("Endpoint not found: " + path));
                    break;
            }
        } catch (Exception e) {
            sendJsonResponse(out, 400, JsonHelper.errorResponse(e.getMessage()));
        }
    }

    private void handleStaticFile(OutputStream out, String rawPath) throws IOException {
        String path = (rawPath == null || rawPath.equals("/") || rawPath.isEmpty()) ? "/index.html" : rawPath;
        Path target = frontendPath.resolve("." + path).normalize();

        if (!target.startsWith(frontendPath) || !Files.exists(target) || Files.isDirectory(target)) {
            String error404 = "<h1>404 Not Found</h1><p>File not found: " + path + "</p>";
            byte[] bytes = error404.getBytes(StandardCharsets.UTF_8);
            sendRawResponse(out, 404, "text/html; charset=UTF-8", bytes);
            return;
        }

        byte[] fileBytes = Files.readAllBytes(target);
        String mime = getMimeType(target.getFileName().toString());
        sendRawResponse(out, 200, mime, fileBytes);
    }

    private void sendCorsPreflightResponse(OutputStream out) throws IOException {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        pw.print("HTTP/1.1 204 No Content\r\n");
        pw.print("Access-Control-Allow-Origin: *\r\n");
        pw.print("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n");
        pw.print("Access-Control-Allow-Headers: Content-Type, Authorization\r\n");
        pw.print("Content-Length: 0\r\n");
        pw.print("Connection: close\r\n\r\n");
        pw.flush();
    }

    private void sendJsonResponse(OutputStream out, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        sendRawResponse(out, status, "application/json; charset=UTF-8", bytes);
    }

    private void sendRawResponse(OutputStream out, int status, String mime, byte[] content) throws IOException {
        String statusText = (status == 200) ? "OK" : ((status == 404) ? "Not Found" : ((status == 405) ? "Method Not Allowed" : "Bad Request"));
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        pw.print("HTTP/1.1 " + status + " " + statusText + "\r\n");
        pw.print("Content-Type: " + mime + "\r\n");
        pw.print("Content-Length: " + content.length + "\r\n");
        pw.print("Access-Control-Allow-Origin: *\r\n");
        pw.print("Connection: close\r\n\r\n");
        pw.flush();
        out.write(content);
        out.flush();
    }

    private String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
