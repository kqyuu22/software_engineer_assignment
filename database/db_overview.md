# University Parking System: Database Architecture & Spring Boot Integration Guide
This document outlines the schema, constraints, and automated business logic for the university parking system, specifically tailored for a Spring Boot backend environment. Because this architecture utilizes a "database-first" design with native triggers and advanced indexes, the Spring Boot application must be configured as a lightweight orchestrator rather than the sole source of truth for business logic.

## 1. Spring Boot Integration Mandates
To prevent your Java application from fighting or breaking the database architecture, your developers must adhere to the following configurations.

Restrict Schema Auto-Generation: Hibernate defaults to modifying database schemas automatically, which will destroy our custom triggers and partial indexes. You must set spring.jpa.hibernate.ddl-auto=validate (or none) in your application.properties to force Spring to strictly respect the existing PostgreSQL schema.

Delegate Primary Key Generation: The database manages all IDs using modern PostgreSQL sequences. Your JPA entities must map their primary keys using @Id alongside @GeneratedValue(strategy = GenerationType.IDENTITY) to ensure Hibernate retrieves the ID created by the database rather than attempting to generate its own.

Map PostgreSQL ENUMs Native Types: Standard JPA maps Java Enums to generic VARCHAR columns, which will fail against our strict native PostgreSQL ENUMs. You should utilize a library like Hypersistence Utils (formerly Hibernate Types) and apply @Type(PostgreSQLEnumType.class) on your entity Enum fields to bridge this gap cleanly.

Handle Constraint Exceptions Gracefully: When a user attempts to park a car that is already active, the database's unique indexes will violently reject the transaction. Do not let this surface as an HTTP 500 server error. Implement a @RestControllerAdvice class with an @ExceptionHandler(DataIntegrityViolationException.class) to catch these rejections and return a clean HTTP 400 Bad Request to the frontend.

Do Not Duplicate Business Logic: Developers must not calculate parking fees or generate billing records in Java. All mathematical and routing logic is locked in the database layer to prevent concurrency race conditions.

## 2. Core Architectural Rules
Four-Digit SSO IDs: All Single Sign-On (SSO) users are mathematically guaranteed to have an exact 4-digit ID (1000 to 9999). This is inherently enforced by an integer boundary check.

Split Ticketing: Authenticated university members and transient guests are handled in two completely separate tables (sso_tickets and guest_tickets). This eliminates fragile polymorphic associations and NULL column bloat.

Monthly Aggregated Billing: SSO users are never billed per ticket. Parking fees are automatically accumulated into centralized monthly bills via database automation.

Dynamic Base Pricing: The base parking fee is stored centrally in the price table. Unless a specific penalty fee is overridden by the operator during insertion, tickets dynamically fetch the live price at the exact moment of entry.

## 3. Schema Definitions & JPA Setup
Users & Authentication
sso_users: The core authentication table. The username field holds a strict unique constraint.

uni_members: An extension table mapping specific roles (e.g., STUDENT, LECTURER). It shares the user_id primary key with sso_users via a CASCADE delete constraint, guaranteeing a strict 1:1 relationship. Use @OneToOne and @PrimaryKeyJoinColumn in Hibernate.

Infrastructure
price: A configuration table permanently restricted to a single row (id = 1) that dictates the global parking cost.

parking_slots: Manages the physical 250 spots.

Financials
billing: Stores the aggregated monthly tabs for SSO users. Contains billing_month (always truncated to the first day of the respective month), amount, and status.

Ticketing
sso_tickets: Tickets bound to an authentic sso_user. Links via foreign keys to parking_slots and the billing table.

guest_tickets: Independent tickets for transient visitors paying cash. Includes paid_directly and final_calculated_fee columns to handle on-the-spot transactions independently of the digital billing tables.

all_tickets_view: A unified PostgreSQL VIEW stitching both ticket tables together. Create an immutable @Entity or use a projection interface in Spring Data JPA to query this view for your operator dashboards.

## 4. Automated Checkout Pipeline
When a backend application updates an sso_ticket by setting an exit_time, a PL/pgSQL trigger named process_sso_checkout automatically intercepts the transaction.

It calculates the total hours parked (rounded up to the ceiling hour, with a one-hour minimum). It multiplies those hours by the ticket's registered price. It then determines the correct calendar month and either finds the user's existing unpaid bill or generates a brand new one. Finally, it adds the total cost to the bill and links the records together.

Actionable Backend Code: To check out an SSO user, your Spring Boot service needs exactly one line of logic:
ticketRepository.updateExitTime(ticketId, LocalDateTime.now());
The database handles the rest instantly.

## 5. Security & Concurrency (The Double-Lock)
The database utilizes a two-layered defense mechanism to strictly prevent the same car from parking twice simultaneously, regardless of how many asynchronous threads your Java backend spawns.

Layer 1: Partial Unique Indexes (Same-Table Protection): Standard unique constraints would permanently lock a license plate after one visit. We use partial indexes (WHERE finished = false) to ensure a plate is only unique while the car is actively in the lot.

Layer 2: Radar Triggers (Cross-Table Protection): Because indexes cannot span multiple tables, a custom trigger (check_cross_table_active_plate) fires before any insert. If a guest ticket is being generated, it queries the SSO table to ensure that plate isn't already hiding there, preventing cross-contamination.