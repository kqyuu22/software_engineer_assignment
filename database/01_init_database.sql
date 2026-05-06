-- ==============================================================================
-- 0. DROP THE WHOLE DATABASE
-- ==============================================================================
-- Drop ALL objects in the public schema (tables, enums, triggers, functions, views, etc.)
drop schema if exists public cascade;

-- Recreate empty public schema
create schema if not exists public;

-- (Optional but common) Ensure default grants work
grant usage on schema public to public;

-- ==============================================================================
-- 1. CUSTOM TYPES (ENUMS)
-- Define the restricted vocabularies before they are used in the tables.
-- ==============================================================================
CREATE TYPE alert_type AS ENUM ('SECURITY_BREACH', 'SYSTEM_FAILURE');
CREATE TYPE slot_status AS ENUM ('AVAILABLE', 'RESERVED', 'OCCUPIED', 'UNKNOWN');
CREATE TYPE slot_priority AS ENUM ('STAFF', 'LECTURER', 'STUDENT', 'OTHER');
CREATE TYPE user_role AS ENUM ('MEMBER', 'OPERATOR', 'ADMIN');
CREATE TYPE bill_status AS ENUM ('UNPAID', 'PAID', 'CANCELLED');

-- ==============================================================================
-- 2. BASE CONFIGURATION TABLE
-- Must be created before the helper function queries it.
-- Price is role-based, so we set each unique role as primary key
-- ==============================================================================
CREATE TABLE public.price (
    slot_priority slot_priority PRIMARY KEY,
    price numeric(10, 2) NOT NULL DEFAULT 0.00
);

-- ==============================================================================
-- 3. UTILITY FUNCTIONS
-- Must be created before the ticket tables so they can be used as DEFAULTs.
-- ==============================================================================


-- ==============================================================================
-- 4. CORE ENTITIES
-- Setting up the users and physical parking infrastructure.
-- ==============================================================================
CREATE TABLE public.sso_users (
    user_id integer PRIMARY KEY CHECK (user_id >= 1000 AND user_id <= 9999),
    name character varying NOT NULL,
    username character varying NOT NULL UNIQUE,
    password character varying NOT NULL,
    role user_role NOT NULL
);

CREATE TABLE public.uni_members (
    user_id integer PRIMARY KEY REFERENCES public.sso_users(user_id) ON DELETE CASCADE,
    name character varying,
    role character varying NOT NULL 
);

CREATE TABLE public.parking_slots (
    slot_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status slot_status NOT NULL DEFAULT 'AVAILABLE',
    priority slot_priority NOT NULL DEFAULT 'OTHER'
);

CREATE TABLE public.alerts (
    alert_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type alert_type NOT NULL,
    message text NOT NULL,
    timestamp timestamptz NOT NULL DEFAULT now(),
    acknowledged boolean NOT NULL DEFAULT false
);

-- ==============================================================================
-- 5. MONTHLY BILLING
-- Must exist before SSO tickets to satisfy the foreign key constraint.
-- ==============================================================================
CREATE TABLE public.billing (
    bill_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id integer NOT NULL REFERENCES public.sso_users(user_id) ON DELETE RESTRICT,
    billing_month date NOT NULL,
    amount numeric(10, 2) NOT NULL DEFAULT 0.00,
    status bill_status NOT NULL DEFAULT 'UNPAID',
    last_updated timestamptz NOT NULL DEFAULT now()
);

-- ==============================================================================
-- 6. TICKETING SYSTEM (Split Architecture)
-- Separating authenticated users from transient guests.
-- ==============================================================================
CREATE TABLE public.tickets (
    ticket_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entry_time timestamptz NOT NULL DEFAULT now(),
    exit_time timestamptz,
    license_plate character varying NOT NULL,
    parking_spot integer REFERENCES public.parking_slots(slot_id) ON DELETE SET NULL,
    finished boolean NOT NULL DEFAULT false,
    price numeric(10, 2) NOT NULL DEFAULT 0.0
);

CREATE TABLE public.sso_tickets (
    sso_ticket_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id integer REFERENCES public.tickets(ticket_id) ON DELETE CASCADE,
    user_id integer NOT NULL REFERENCES public.sso_users(user_id) ON DELETE CASCADE,
    bill_id bigint REFERENCES public.billing(bill_id) ON DELETE SET NULL
);

CREATE TABLE public.guest_tickets (
    guest_ticket_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id integer REFERENCES public.tickets(ticket_id) ON DELETE CASCADE,
    final_calculated_fee numeric(10, 2), 
    paid_directly boolean NOT NULL DEFAULT false
);

-- ==============================================================================
-- 7. UNIFIED VIEW
-- Provides the backend operators with a single pane of glass for all tickets.
-- ==============================================================================
CREATE OR REPLACE VIEW public.all_tickets_view AS
SELECT 
    'SSO' AS ticket_type,
    t.ticket_id,
    st.user_id::text AS holder_identifier, 
    t.entry_time,
    t.exit_time,
    t.license_plate,
    t.parking_spot,
    t.finished
FROM public.tickets t
JOIN public.sso_tickets st ON t.ticket_id = st.ticket_id

UNION ALL

SELECT 
    'GUEST' AS ticket_type,
    t.ticket_id,
    -- Updated to use GUEST- + guest_ticket_id
    'GUEST-' || gt.guest_ticket_id::text AS holder_identifier, 
    t.entry_time,
    t.exit_time,
    t.license_plate,
    t.parking_spot,
    t.finished
FROM public.tickets t
JOIN public.guest_tickets gt ON t.ticket_id = gt.ticket_id;