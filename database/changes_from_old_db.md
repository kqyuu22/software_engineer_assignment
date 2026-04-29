## 1. Core Data Type Upgrades
Floating-Point Math Eliminated:

Old: Financial columns (amount, fee, price) used the REAL data type.

New: Upgraded to NUMERIC(10, 2).

Reason: REAL introduces floating-point rounding errors, which is disastrous for financial systems. NUMERIC guarantees exact mathematical precision for currency.

Timezone Blindness Fixed:

Old: Used timestamp without time zone.

New: Upgraded to timestamptz (timestamp with time zone).

Reason: Prevents the backend from having to guess or manually offset timezones across the application.

Identity Generation Standardized:

Old: Mixed usage of legacy nextval('sequence') and modern IDENTITY.

New: All auto-incrementing integer keys now strictly use GENERATED ALWAYS AS IDENTITY.

Reason: Adheres to modern SQL standards and prevents engineers from accidentally bypassing the sequence by manually inserting ID numbers.

## 2. Architectural Restructuring
The Ticketing System (Split Architecture):

Old: A single tickets table that mixed both SSO users and transient guests using string-based roles, lacking proper foreign keys.

New: Split into sso_tickets (with strict foreign keys to the users table) and guest_tickets (fully independent). A unified view (all_tickets_view) was added to stitch them back together for operator dashboards.

Reason: Eliminates the "Polymorphic Association" anti-pattern. Authenticated users and transient guests have completely different data lifecycles and billing structures. Splitting them enforces strict referential integrity.

The Billing System (Per-Ticket vs. Monthly Aggregation):

Old: A payment table mapped 1:1 with individual tickets.

New: Replaced by a billing table. Tickets no longer require immediate payment; their costs are aggregated into monthly bills grouped by a billing_month date field. Guest tickets handle cash on the spot via a final_calculated_fee column.

Reason: Maps to the actual business logic of a university (students and staff pay monthly tabs, not per-exit fees).

## 3. Constraint & Security Enhancements
ENUMs Replaced Verbose Checks:

Old: String columns with massive CHECK (status::text = ANY(...)) arrays.

New: Native PostgreSQL ENUM types (slot_status, user_role, alert_type, etc.).

Reason: ENUMs are type-safe, execute faster, and provide a much cleaner schema definition.

Strict User ID Enforcement:

Old: User IDs were standard auto-incrementing serials, and the 1:1 relationship between sso_users and uni_members was not enforced.

New: The sso_users.user_id is now a standard integer strictly bounded by a CHECK (user_id >= 1000 AND user_id <= 9999). The uni_members table shares this exact ID via a REFERENCES ... ON DELETE CASCADE constraint.

Reason: Mathematically guarantees the business rule that all SSO IDs must be exactly 4 digits without leading zeros.

Dynamic Base Pricing:

Old: The base parking fee was hardcoded as DEFAULT 5000 in the table definitions.

New: A SQL helper function get_current_base_price() dynamically reads the live price from the public.price table and applies it as the default.

Reason: Allows administrators to update the parking price globally by changing one row, without requiring developers to alter table schemas.

The Double-Lock Concurrency Protection:

Old: The database allowed the same license plate to be parked multiple times simultaneously (Ghost tickets).

New: Added Partial Unique Indexes (WHERE finished = false) on both ticket tables, reinforced by a cross-table PL/pgSQL trigger (check_cross_table_active_plate).

Reason: Physically prevents race conditions if backend servers process duplicate entry scans simultaneously.

## 4. Automation Offloaded to the Database
Automated Checkout & Billing Math:

Old: The backend application was expected to calculate hours, multiply fees, and generate payment records.

New: A PL/pgSQL trigger (process_sso_checkout) fires automatically when a ticket's exit_time is updated. It calculates the ceiling hours, reads the dynamic price, finds the correct monthly bill, and adds the total.

Reason: Centralizes business logic, guarantees absolute mathematical consistency regardless of API bugs, and drastically simplifies the Spring Boot codebase.