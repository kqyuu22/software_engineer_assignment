# Database Schema Migration Guide: Legacy to V2
Context for AI Code Generator: This document maps a legacy PostgreSQL schema to a newly normalized V2 architecture. Use this mapping to generate updated JPA Entities, Spring Data Repositories, and DTOs.

Critical Directives for Code Generation:

Do not generate backend logic for calculating parking fees, durations, or monthly billing accumulations. This is handled entirely by database triggers.

Map all primary keys using GenerationType.IDENTITY.

Map all Enums using native PostgreSQL ENUM types (e.g., via Hypersistence Utils).

Expect DataIntegrityViolationException on ticket inserts due to strict partial unique indexes on active license plates.

## 1. Global Data Type Migrations
Apply these type changes universally across all entity fields:

REAL ➔ NUMERIC(10, 2) (Use java.math.BigDecimal).

TIMESTAMP ➔ TIMESTAMPTZ (Use java.time.OffsetDateTime or ZonedDateTime).

SERIAL ➔ IDENTITY (Use GenerationType.IDENTITY).

## 2. Native Enum Definitions
Create Java Enums for the following native PostgreSQL types:

user_role: MEMBER, OPERATOR, ADMIN

slot_status: AVAILABLE, RESERVED, OCCUPIED, UNKNOWN

slot_priority: STAFF, LECTURER, STUDENT, OTHER

bill_status: UNPAID, PAID, CANCELLED

alert_type: SECURITY_BREACH, SYSTEM_FAILURE

## 3. Entity Mapping (Old vs. New Signatures)
Domain: Users & Authentication

Table: sso_users

```sql
-- OLD SIGNATURE
sso_users (user_id SERIAL, name VARCHAR, username VARCHAR, password VARCHAR, role VARCHAR)

-- NEW SIGNATURE
sso_users (
    user_id INTEGER PRIMARY KEY, -- Checked: 1000 to 9999
    name VARCHAR NOT NULL,
    username VARCHAR NOT NULL UNIQUE,
    password VARCHAR NOT NULL,
    role USER_ROLE_ENUM NOT NULL
)
```
Table: uni_members

```sql
-- OLD SIGNATURE
uni_members (user_id INTEGER, name VARCHAR, role VARCHAR)

-- NEW SIGNATURE
uni_members (
    user_id INTEGER PRIMARY KEY, -- FK to sso_users(user_id) ON DELETE CASCADE
    name VARCHAR,
    role VARCHAR NOT NULL
)
```

Domain: Infrastructure & Operations

Table: parking_slots

```sql
-- OLD SIGNATURE
parking_slots (slot_id IDENTITY, status VARCHAR, priority VARCHAR)

-- NEW SIGNATURE
parking_slots (
    slot_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status SLOT_STATUS_ENUM NOT NULL DEFAULT 'AVAILABLE',
    priority SLOT_PRIORITY_ENUM NOT NULL DEFAULT 'OTHER'
)
```

Table: price

```sql
-- OLD SIGNATURE
price (id INTEGER, price REAL)

-- NEW SIGNATURE
price (
    id INTEGER PRIMARY KEY DEFAULT 1, -- Checked: id = 1
    price NUMERIC(10, 2) NOT NULL DEFAULT 0.00
)
```

Table: alerts

```sql
-- OLD SIGNATURE
alerts (alert_id SERIAL, type VARCHAR, message VARCHAR, timestamp TIMESTAMP, acknowledged BOOLEAN)

-- NEW SIGNATURE
alerts (
    alert_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type ALERT_TYPE_ENUM NOT NULL,
    message TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT now(),
    acknowledged BOOLEAN NOT NULL DEFAULT false
)
```

Domain: Financials

Table: billing (Replaces old payment table)

```sql
-- OLD SIGNATURE (payment)
payment (id IDENTITY, user_id BIGINT, amount REAL, status VARCHAR, timestamp TIMESTAMP)

-- NEW SIGNATURE (billing)
billing (
    bill_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INTEGER NOT NULL, -- FK to sso_users
    billing_month DATE NOT NULL,
    amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    status BILL_STATUS_ENUM NOT NULL DEFAULT 'UNPAID',
    last_updated TIMESTAMPTZ NOT NULL DEFAULT now()
)
```

Domain: Ticketing (Split Architecture)

Table: sso_tickets (Replaces tickets for Members)

```sql
-- OLD SIGNATURE (tickets)
tickets (ticket_id SERIAL, user_id INTEGER, role VARCHAR, entry_time TIMESTAMP, exit_time TIMESTAMP, license_plate VARCHAR, parking_spot INTEGER, finished BOOLEAN, fee REAL, price REAL, payment_id BIGINT)

-- NEW SIGNATURE (sso_tickets)
sso_tickets (
    ticket_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INTEGER NOT NULL, -- FK to sso_users
    entry_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    exit_time TIMESTAMPTZ,
    license_plate VARCHAR NOT NULL,
    parking_spot INTEGER, -- FK to parking_slots
    finished BOOLEAN NOT NULL DEFAULT false,
    price NUMERIC(10, 2) NOT NULL, -- Defaults via DB function
    bill_id BIGINT UNIQUE -- FK to billing
)
```

Table: guest_tickets (New Table for Non-Members)

```sql
-- NEW SIGNATURE
guest_tickets (
    ticket_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entry_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    exit_time TIMESTAMPTZ,
    license_plate VARCHAR NOT NULL,
    parking_spot INTEGER, -- FK to parking_slots
    finished BOOLEAN NOT NULL DEFAULT false,
    price NUMERIC(10, 2) NOT NULL, -- Defaults via DB function
    final_calculated_fee NUMERIC(10, 2),
    paid_directly BOOLEAN NOT NULL DEFAULT false
)
```

View: all_tickets_view (Read-Only Projection)

```sql
-- NEW SIGNATURE
all_tickets_view (
    ticket_type TEXT, -- 'SSO' or 'GUEST'
    ticket_id INTEGER,
    holder_identifier TEXT, -- user_id or 'GUEST-{id}'
    entry_time TIMESTAMPTZ,
    exit_time TIMESTAMPTZ,
    license_plate VARCHAR,
    parking_spot INTEGER,
    finished BOOLEAN
)
```