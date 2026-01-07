# Automated Payment Reconciliation System (CLI)

Enterprise-grade CLI tool for reconciling finance invoices with bank payments.

## Tech Stack
- Java 21
- Spring Boot 3.2
- PostgreSQL
- Maven

HOW EVERTHING WORKS

1. Executive Summary
What is this tool? This is a software application designed for finance teams. It replaces the manual work of staring at Excel spreadsheets to match "Invoices Sent" with "Payments Received" from the bank.

What does it do?

Reads Data: It takes CSV files containing thousands of invoices and bank transactions.

Matches Them: It automatically figures out which payment belongs to which invoice using logic (Exact amounts, Invoice numbers).

Finds Problems: It flags partial payments or missing payments for human review.

Reports: It creates a summary of how much money has been successfully reconciled.

2. How It Works (The "Big Picture")
Imagine a funnel. You pour raw data into the top. Inside the funnel, the system organizes the data, compares it, and filters it. At the bottom, clean, matched reports come out.

The Workflow:

Ingestion: The system reads invoices.csv and payments.csv and saves them into a database (PostgreSQL).

Processing: The "Engine" wakes up, looks at every unmatched payment, and hunts for a matching invoice.

Reporting: The system calculates the results and prints a summary for the user.

3. Code Walkthrough (File by File)
Here is an explanation of every file in the project, written in plain English.

🛠️ Configuration & Setup
1. pom.xml (The Shopping List)

Analogy: This is the recipe card or shopping list for the project.

What it does: It tells the computer, "I need Java 21, Spring Boot, and the Postgres driver." It ensures all the necessary tools are downloaded before we start cooking.

2. application.properties (The Settings Menu)

Analogy: The settings screen on your phone.

What it does: It holds critical secrets, like the database username (postgres) and password. It also tells the system how to behave (e.g., "Don't show me too many messy logs, just errors").

3. ReconciliationApplication.java (The Commander)

Analogy: The Traffic Cop or the Main Entrance.

What it does: When you run the program, this file runs first. It looks at what command you typed (e.g., run-all or report) and directs the traffic to the right department.

🗄️ The Data Models (The "Forms")
4. Invoice.java & Payment.java

Analogy: Blank forms or templates.

What it does: These define what an "Invoice" looks like (Number, Amount, Date) and what a "Payment" looks like. They map these Java objects to real tables in the database.

5. AuditLog.java

Analogy: A security camera or a diary.

What it does: Every time the system does something important (like "Ingested 50 files"), it writes a permanent entry here. This is crucial for financial compliance so auditors can see what happened.

6. ReconciliationResult.java

Analogy: A paperclip stapling two documents together.

What it does: When the system finds a match, it creates this record to link a specific Payment ID to a specific Invoice ID permanently.

💾 The Repositories (The Librarians)
7. InvoiceRepository.java / PaymentRepository.java

Analogy: Librarians.

What it does: The rest of the app doesn't know how to talk to the database directly. It asks these repositories: "Hey, find me all invoices that are unpaid." The repository goes to the database, fetches them, and brings them back.

⚙️ The Services (The Workers)
8. IngestionService.java (The Data Entry Clerk)

Analogy: A fast typist.

What it does: It opens the CSV files, reads them line-by-line, checks if the data is valid (e.g., is the amount a number?), and hands it to the Repository to save. It skips bad rows so the whole system doesn't crash.

9. ReconciliationService.java (The Detective)

Analogy: Sherlock Holmes.

What it does: This is the brain. It pulls up a list of unmatched payments and invoices. It runs logic: "Does this payment reference number match an invoice number?" If yes, match them. If no, "Do the amounts and dates match?" It decides if a payment is MATCHED or PARTIAL.

10. ReportingService.java (The Analyst)

Analogy: The person who writes the daily memo.

What it does: It doesn't change data; it just reads it. It counts how many items are matched vs. unmatched and prints the pretty summary box you see in the terminal.

4. Why Is This "Enterprise Grade"?
You might wonder why we have 15 files instead of just one big script.

Safety: If the database crashes, the code handles it gracefully.

Scalability: We use "Batch Processing" (handling 100 items at a time). If we tried to load 1 million records at once, a simple script would crash your computer. This code won't.

Auditability: In finance, you can't just change numbers. We track every action in the AuditLog so no one can "cook the books" without leaving a trace.

Maintainability: The "Detective" logic is separate from the "Data Entry" logic. If we need to change how we read CSVs later, we don't risk breaking the matching logic.

5. How to Read the Final Output
When you saw this:

Plaintext

Total Fully Matched : 2
Total Partial Match : 1
Pending / Unmatched : 1
Fully Matched: The system is 100% sure the customer paid the bill. No human action needed.

Partial Match: The customer paid, but the amount was slightly off (maybe bank fees or a mistake). A human needs to look at this quickly to approve it.

Unmatched: The system has no idea who sent this money. A human needs to investigate.

Result: instead of reviewing 4 records manually, the human only had to review 1 or 2. In a real company with 10,000 records, this saves hundreds of hours of work.
