[README.md](https://github.com/user-attachments/files/34920896/README.md)
# 🏦 ATM Simulation System

A full-stack, modular **ATM Simulation System** with clean separation across **Frontend (HTML & CSS)**, **Backend (Java)**, and **Database (SQL)**.

---

## 📁 Project Folder Structure

```text
uday project/
│
├── frontend/                     # FRONTEND (User Interface)
│   ├── index.html                # Welcome & Card Insertion Screen
│   ├── pin.html                  # 4-Digit Secure PIN Verification
│   ├── dashboard.html            # Main ATM Menu (Withdraw, Deposit, Balance, etc.)
│   ├── balance.html              # Balance Inquiry Screen & Receipt Print
│   ├── withdraw.html             # Cash Withdrawal (Fast cash + Custom amounts)
│   ├── deposit.html              # Instant Cash Deposit Screen
│   ├── statement.html            # Mini Statement (Last 10 Transactions)
│   ├── change-pin.html           # Change ATM PIN Screen
│   ├── css/
│   │   └── style.css             # High-tech ATM Kiosk theme, keypad & animations
│   └── js/
│       └── app.js                # API connector, keypad audio clicks & session storage
│
├── backend/                      # BACKEND (Java Server & Business Logic)
│   ├── pom.xml                   # Maven project descriptor (IntelliJ / Eclipse / VS Code)
│   ├── run.bat                   # 1-Click Windows batch script to compile & run
│   ├── run.ps1                   # PowerShell run script
│   └── src/main/java/com/atm/
│       ├── AtmApplication.java   # Main application entry point
│       ├── server/
│       │   └── AtmHttpServer.java# Embedded HTTP 1.1 server (zero external dependency)
│       ├── config/
│       │   ├── DatabaseConnection.java # JDBC MySQL configuration & failover
│       │   └── CorsFilter.java   # CORS headers handler
│       ├── controller/
│       │   ├── AccountController.java  # REST API handlers (/api/auth, /api/atm)
│       │   └── StaticFileHandler.java  # Static asset web server
│       ├── dao/
│       │   ├── AccountDAO.java   # Data Access Object for bank accounts
│       │   └── TransactionDAO.java # Data Access Object for transactions
│       ├── model/
│       │   ├── Account.java      # Account entity
│       │   └── Transaction.java  # Transaction entity
│       ├── service/
│       │   └── AtmService.java   # ATM business rules & validations
│       └── util/
│           └── JsonHelper.java   # Lightweight JSON serializer/parser
│
├── database/                     # DATABASE (SQL Scripts)
│   ├── schema.sql                # DDL: users, accounts, and transactions tables
│   ├── sample_data.sql           # DML: Pre-seeded accounts, test cards & transactions
│   └── queries.sql               # Common test queries (balance, deposit, withdrawal)
│
├── start-project.bat             # 1-Click Project Launcher (Starts backend & opens browser)
└── README.md                     # Documentation & Setup Guide
```

---

## ⚡ Quick Start (Run in 10 Seconds)

### Option 1: Double-Click Launcher (Easiest)
1. Double-click **`start-project.bat`** in the project root folder.
2. The server will start and automatically open **`http://localhost:8080/`** in your default web browser!

---

### Option 2: Command Line (Java)
1. Open a terminal in the project directory:
   ```cmd
   cd backend
   run.bat
   ```
2. Open your browser and navigate to:
   ```
   http://localhost:8080/
   ```

---

## 💳 Demo Test Accounts

The system comes pre-configured with ready-to-test ATM cards (works in both MySQL and in-memory mock mode):

| Card Number | Default PIN | Account Holder | Initial Balance | Account Type |
| :--- | :--- | :--- | :--- | :--- |
| **`1234567890123456`** | **`1234`** | Uday Kumar | **₹ 50,000.00** | SAVINGS |
| **`9876543210987654`** | **`4321`** | Alice Smith | **₹ 25,000.00** | SAVINGS |
| **`1111222233334444`** | **`9999`** | John Doe | **₹ 120,000.00** | CURRENT |

---

## 🗄️ Database Setup (MySQL)

If you have MySQL installed:
1. Open **MySQL Workbench** or command line.
2. Execute the scripts in the `database/` folder:
   ```sql
   source database/schema.sql;
   source database/sample_data.sql;
   ```
3. If your MySQL username/password is different from `root` / `root`, edit `DatabaseConnection.java` under `backend/src/main/java/com/atm/config/DatabaseConnection.java`:
   ```java
   private static final String USER = "root";
   private static final String PASSWORD = "your_mysql_password";
   ```
> *Note: If MySQL is not running or not yet installed, the Java backend automatically uses built-in in-memory storage so the system runs immediately without crashing.*

---

## 🚀 Key Features
- **Realistic ATM Terminal UI**: Digital ATM screen, live clock, card reader slot, receipt printer, and interactive numeric keypad with tactile sound effects.
- **PIN Verification**: Masked 4-digit PIN input with attempt limits.
- **Cash Withdrawal**: Fast Cash preset buttons (₹500, ₹1000, ₹2000, ₹5000) and custom amounts with denomination checks (multiples of ₹100).
- **Cash Deposit**: Immediate account balance credit.
- **Balance Inquiry**: Live balance display and receipt printing simulation.
- **Mini Statement**: Displays recent account transactions with debit/credit badges.
- **PIN Change**: Secure PIN update workflow.
