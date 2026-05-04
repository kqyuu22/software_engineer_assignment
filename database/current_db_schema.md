# Database Schema Overview

This document provides a comprehensive overview of the current database schema for the Smart Parking System (SEBTL).

# Note
- 1. If there are changes in ticket logic
    - Adjust table `ticket` where we have the common logic/information for a ticket
    - If you want to add/delete more types of ticket: add more table inheriting from `ticket` (holding foreign key reference to `ticket.ticket_id`) 
- 2. Table `price` has a role-based feature, so only inserting one more row if there is a new role.

## 1. Enums (Custom Types)
The database utilizes custom enumeration types to enforce restricted vocabularies and ensure data consistency:

- **`alert_type`**: `SECURITY_BREACH`, `SYSTEM_FAILURE`
- **`slot_status`**: `AVAILABLE`, `RESERVED`, `OCCUPIED`, `UNKNOWN`
- **`slot_priority`**: `STAFF`, `LECTURER`, `STUDENT`, `OTHER`
- **`user_role`**: `MEMBER`, `OPERATOR`, `ADMIN`
- **`bill_status`**: `UNPAID`, `PAID`, `CANCELLED`

## 2. Configuration & Utilities
### `price`
Maintains the role-based pricing configuration.
- `slot_priority` (PK, slot_priority): The role/priority.
- `price` (numeric): The price amount in VND.

## 3. Core Entities
### `sso_users`
Stores user credentials and roles for the Single Sign-On (SSO) system.
- `user_id` (integer, PK): Fixed between 1000 and 9999.
- `name` (varchar): Full name.
- `username` (varchar, UNIQUE): Login username.
- `password` (varchar): Password.
- `role` (user_role): Role in the system (MEMBER, OPERATOR, ADMIN).

### `uni_members`
Stores details specific to university members.
- `user_id` (integer, PK, FK -> sso_users.user_id): References the SSO user.
- `name` (varchar): Member's name.
- `role` (varchar): University role.

### `parking_slots`
Represents physical parking slots.
- `slot_id` (integer, PK): Auto-incremented ID.
- `status` (slot_status): Current slot status (default 'AVAILABLE').
- `priority` (slot_priority): Assigned role priority (default 'OTHER').

### `alerts`
Stores system alerts.
- `alert_id` (integer, PK): Auto-incremented ID.
- `type` (alert_type): Type of alert.
- `message` (text): Alert description.
- `timestamp` (timestamptz): When the alert occurred.
- `acknowledged` (boolean): Resolution state.

## 4. Ticketing & Billing Architecture
The ticketing system is split between authenticated members and transient guests.

### `tickets`
The core parking representation.
- `ticket_id` (integer, PK): Auto-incremented ID.
- `entry_time` (timestamptz): Vehicle entry time.
- `exit_time` (timestamptz): Vehicle exit time (nullable).
- `license_plate` (varchar): Registered license plate.
- `parking_spot` (integer, FK -> parking_slots.slot_id): Assigned slot.
- `finished` (boolean): Whether the parking session has concluded.
- `price` (numeric): Calculated price for this session.

### `billing`
Maintains monthly billing records for SSO users.
- `bill_id` (bigint, PK): Auto-incremented ID.
- `user_id` (integer, FK -> sso_users.user_id): The associated user.
- `billing_month` (date): The month this bill applies to.
- `amount` (numeric): Computed total amount.
- `status` (bill_status): Payment status.

### `sso_tickets`
Associates base tickets with SSO users.
- `sso_ticket_id` (integer, PK): Auto-incremented ID.
- `ticket_id` (integer, FK -> tickets.ticket_id): The base ticket reference.
- `user_id` (integer, FK -> sso_users.user_id): The user holding the ticket.
- `bill_id` (bigint, FK -> billing.bill_id): The billing cycle it belongs to.

### `guest_tickets`
Associates base tickets for unregistered guests.
- `guest_ticket_id` (integer, PK): Auto-incremented ID.
- `ticket_id` (integer, FK -> tickets.ticket_id): The base ticket reference.
- `final_calculated_fee` (numeric): Final resolved fee upon exit.
- `paid_directly` (boolean): Payment status.

## 5. Unified Views
### `all_tickets_view`
A merged view intended for Operators and Administration to observe all active and historical tickets comprehensively.
- Merges `sso_tickets` and `guest_tickets` showing unified schema fields.
- Additional fields: `ticket_type` ('SSO' or 'GUEST') and `holder_identifier` (Actual User ID or generated Guest ID format).