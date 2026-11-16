package com.atm;

import com.atm.server.AtmHttpServer;

public class AtmApplication {

    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            AtmHttpServer server = new AtmHttpServer(PORT);
            server.start();

            // Keep main thread alive
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Fatal error in ATM Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
