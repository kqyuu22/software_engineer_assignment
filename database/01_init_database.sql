-- ==============================================================================
-- 0. DROP THE WHOLE DATABASE
-- ==============================================================================
do $$ declare
    r record;
begin
    for r in (select tablename from pg_tables where schemaname = 'my-schema-name') loop
        execute 'drop table if exists ' || quote_ident(r.tablename) || ' cascade';
    end loop;
end $$;

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
-- ==============================================================================
CREATE TABLE public.price (
    id integer PRIMARY KEY CHECK (id = 1) DEFAULT 1,
    price numeric(10, 2) NOT NULL DEFAULT 0.00
);

-- ==============================================================================
-- 3. UTILITY FUNCTIONS
-- Must be created before the ticket tables so they can be used as DEFAULTs.
-- ==============================================================================
CREATE OR REPLACE FUNCTION get_current_base_price()
RETURNS numeric(10, 2) AS $$
    SELECT price FROM public.price WHERE id = 1;
$$ LANGUAGE sql;

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
CREATE TABLE public.sso_tickets (
    ticket_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id integer NOT NULL REFERENCES public.sso_users(user_id) ON DELETE CASCADE,
    entry_time timestamptz NOT NULL DEFAULT now(),
    exit_time timestamptz,
    license_plate character varying NOT NULL,
    parking_spot integer REFERENCES public.parking_slots(slot_id) ON DELETE SET NULL,
    finished boolean NOT NULL DEFAULT false,
    price numeric(10, 2) NOT NULL DEFAULT get_current_base_price(), 
    bill_id bigint REFERENCES public.billing(bill_id) ON DELETE SET NULL
);

CREATE TABLE public.guest_tickets (
    ticket_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entry_time timestamptz NOT NULL DEFAULT now(),
    exit_time timestamptz,
    license_plate character varying NOT NULL,
    parking_spot integer REFERENCES public.parking_slots(slot_id) ON DELETE SET NULL,
    finished boolean NOT NULL DEFAULT false,
    price numeric(10, 2) NOT NULL DEFAULT get_current_base_price(),
    final_calculated_fee numeric(10, 2), 
    paid_directly boolean NOT NULL DEFAULT false
);

-- ==============================================================================
-- 7. UNIFIED VIEW
-- Provides the backend operators with a single pane of glass for all tickets.
-- ==============================================================================
CREATE VIEW public.all_tickets_view AS
SELECT 
    'SSO' AS ticket_type,
    ticket_id,
    user_id::text AS holder_identifier, 
    entry_time,
    exit_time,
    license_plate,
    parking_spot,
    finished
FROM public.sso_tickets
UNION ALL
SELECT 
    'GUEST' AS ticket_type,
    ticket_id,
    'GUEST-' || ticket_id::text AS holder_identifier, 
    entry_time,
    exit_time,
    license_plate,
    parking_spot,
    finished
FROM public.guest_tickets;
