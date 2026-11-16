package com.atm.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Default MySQL configuration (adjust to your local MySQL settings)
    private static final String URL = "jdbc:mysql://localhost:3306/atm_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "root"; // Update to your MySQL password if needed

    private static boolean driverLoaded = false;
    private static boolean connectionFailedWarned = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            // Driver not in classpath; DAO will fallback to in-memory mode
            driverLoaded = false;
        }
    }

    public static boolean isDriverAvailable() {
        return driverLoaded;
    }

    public static Connection getConnection() {
        if (!driverLoaded) {
            return null;
        }
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            if (!connectionFailedWarned) {
                System.out.println("[DB Notice] Could not connect to MySQL (" + e.getMessage() + ").");
                System.out.println("[DB Notice] Operating with built-in in-memory ATM storage so everything works seamlessly out of the box!");
                System.out.println("[DB Notice] To connect to MySQL: import database/schema.sql and database/sample_data.sql into your MySQL server.");
                connectionFailedWarned = true;
            }
            return null;
        }
    }
}
