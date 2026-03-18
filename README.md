Automated Payment Reconciliation System

Setup and Execution Guide

This guide explains how to set up and run the Automated Payment Reconciliation System on your local machine.

1. Prerequisites
Before running the project, ensure the following software is installed on your system:

Java (JDK 17 or later)
Apache Maven
Python 3
PostgreSQL
Visual Studio Code (VS Code)

2. Database Setup

Open PostgreSQL.

Create a new database with the following name:

financial_db

Update the database password in the Spring Boot configuration file so it matches your PostgreSQL credentials.

Example (application.properties):

spring.datasource.url=jdbc:postgresql://localhost:5432/financial_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
Make sure the password is correct, otherwise the application will fail to connect to the database.

3. Opening the Project

* Open Visual Studio Code.
* Open the entire project folder.
* Open the VS Code Terminal.
* Paste all the required project code files in the project directory if not already present.

4. Running the Application

Follow the steps in the exact order below.

Step 1 — Start the Spring Boot Backend

Run the following command in the VS Code terminal:

* mvn spring-boot:run
This will start the Spring Boot backend server, which handles:
API endpoints
Database interaction
Payment and invoice reconciliation logic

Step 2 — Launch the Dashboard

Run the following command to open the dashboard:

* start "C:\Users\kosht\OneDrive\Desktop\PaymentReconciliationCLI\dashboard\index.html"

This will open the web dashboard in your browser where you can view reconciliation results.

Step 3 — Start Invoice Data Generator

Run the following Python script:

*python generate_invoices.py

This script continuously generates dummy invoice data and sends it to the system through the API.

Step 4 — Start Payment Data Generator

Run the following Python script:

* python generate_payments.py

This script generates dummy payment data and sends it to the system through the API.

5. System Workflow

After all components are running:

Invoice Generator
Creates random invoice records.
Payment Generator
Creates random payment records.
Spring Boot Backend
Receives the data via APIs.
Stores it in the PostgreSQL database.
Runs the reconciliation algorithm.


Verify the API endpoints are reachable before running the Python generators.
