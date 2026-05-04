-- ==============================================================================
-- 0. WIPE EXISTING DATA (The "Clean Slate" Protocol)
-- This destroys all data in these tables and resets all auto-incrementing IDs to 1.
-- CASCADE ensures foreign key dependencies are safely wiped as well.
-- ==============================================================================
TRUNCATE TABLE 
    public.price, 
    public.parking_slots, 
    public.sso_users, 
    public.billing, 
    public.tickets,
    public.sso_tickets, 
    public.guest_tickets, 
    public.alerts 
RESTART IDENTITY CASCADE;

-- ==============================================================================
-- 1. INITIALIZE CONFIGURATIONS
-- ==============================================================================
INSERT INTO public.price (slot_priority, price) VALUES 
('STUDENT', 2000.0),
('LECTURER', 3000.0),
('STAFF', 4000.0),
('OTHER', 5000.0);

-- ==============================================================================
-- 2. SEED PARKING SLOTS
-- ==============================================================================
INSERT INTO public.parking_slots (status, priority)
SELECT 
    'AVAILABLE'::slot_status,
    CASE
        WHEN i <= 50 THEN 'STAFF'::slot_priority
        WHEN i <= 100 THEN 'LECTURER'::slot_priority
        WHEN i <= 200 THEN 'STUDENT'::slot_priority
        ELSE 'OTHER'::slot_priority
    END
FROM generate_series(1, 250) AS i;

-- Occupy specific slots for our active tickets below
UPDATE public.parking_slots SET status = 'OCCUPIED' WHERE slot_id IN (1, 10, 101, 201);

-- ==============================================================================
-- 3. SEED USERS & UNI MEMBERS 
-- ==============================================================================
INSERT INTO public.sso_users (user_id, name, username, password, role) VALUES
    (1001, 'Nguyen Van A', 'member01', '123', 'MEMBER'),
    (1002, 'Tran Thi B', 'member02', 'password123', 'MEMBER'),
    (1003, 'Le Van C', 'operator01', 'password123', 'OPERATOR'),
    (1004, 'Pham Thi D', 'operator02', 'password123', 'OPERATOR'),
    (1005, 'Hoang Van E', 'admin01', 'password123', 'ADMIN'),
    (1006, 'Mr. STAFF', 'staff01', '1001', 'MEMBER'),
    (2001, 'Bui Thi F', 'student01', 'password123', 'MEMBER'),
    (2002, 'Vu Van G', 'student02', 'password123', 'MEMBER'),
    (2003, 'Dang Thi H', 'student03', 'password123', 'MEMBER'),
    (2004, 'Ngo Van I', 'student04', 'password123', 'MEMBER'),
    (2005, 'Ly Thi K', 'student05', 'password123', 'MEMBER');

INSERT INTO public.uni_members (user_id, name, role) VALUES
    (1001, 'Nguyen Van A', 'STAFF'),
    (1002, 'Tran Thi B', 'LECTURER'),
    (1003, 'Le Van C', 'OTHER'),
    (1004, 'Pham Thi D', 'LECTURER'),
    (2001, 'Bui Thi F', 'STUDENT'),
    (2002, 'Vu Van G', 'STUDENT'),
    (2003, 'Dang Thi H', 'STUDENT'),
    (2004, 'Ngo Van I', 'STUDENT'),
    (2005, 'Ly Thi K', 'STUDENT');

-- ==============================================================================
-- 4. SEED MONTHLY BILLING
-- ==============================================================================
INSERT INTO public.billing (user_id, billing_month, amount, status) VALUES
    (1006, '2026-04-01', 10000.00, 'UNPAID'), 
    (1001, '2026-04-01', 26000.00, 'UNPAID'); 

-- ==============================================================================
-- 5. SEED SSO TICKETS
-- ==============================================================================
-- Insert Active SSO Ticket 1
WITH t AS (
  INSERT INTO public.tickets (entry_time, exit_time, license_plate, parking_spot, finished)
  VALUES ('2026-04-25 22:15:40', NULL, '55BB34567', 10, false)
  RETURNING ticket_id
)
INSERT INTO public.sso_tickets (ticket_id, user_id) 
SELECT ticket_id, 1001 FROM t;

-- Insert Active SSO Ticket 2
WITH t AS (
  INSERT INTO public.tickets (entry_time, exit_time, license_plate, parking_spot, finished)
  VALUES ('2026-04-22 01:35:46.799', NULL, '59L-13431', 1, false)
  RETURNING ticket_id
)
INSERT INTO public.sso_tickets (ticket_id, user_id) 
SELECT ticket_id, 1006 FROM t;

-- Insert Active SSO Ticket 3 (Dynamic Time)
WITH t AS (
  INSERT INTO public.tickets (entry_time, exit_time, license_plate, parking_spot, finished)
  VALUES (now() - interval '3 hours', NULL, '29C-12345', 101, false)
  RETURNING ticket_id
)
INSERT INTO public.sso_tickets (ticket_id, user_id) 
SELECT ticket_id, 2001 FROM t;

-- Insert Finished SSO Ticket
WITH t AS (
  INSERT INTO public.tickets (entry_time, exit_time, license_plate, parking_spot, finished)
  VALUES ('2026-04-25 22:19:09', '2026-04-26 02:30:26.1262', '55BB12345', NULL, true)
  RETURNING ticket_id
)
INSERT INTO public.sso_tickets (ticket_id, user_id) 
SELECT ticket_id, 1001 FROM t;

-- ==============================================================================
-- 6. SEED GUEST TICKETS
-- Assigned to slots in the 201-250 "OTHER" range.
-- ==============================================================================
-- Active Guest Ticket
WITH t AS (
  INSERT INTO public.tickets (entry_time, exit_time, license_plate, parking_spot, finished)
  VALUES (now() - interval '2 hours', NULL, '30A-99999', 201, false)
  RETURNING ticket_id
)
INSERT INTO public.guest_tickets (ticket_id, final_calculated_fee, paid_directly)
SELECT ticket_id, NULL, false FROM t;

-- Finished & Paid Guest Ticket
WITH t AS (
  INSERT INTO public.tickets (entry_time, exit_time, license_plate, parking_spot, finished)
  VALUES (now() - interval '6 hours', now() - interval '2 hours', '29B-88888', NULL, true)
  RETURNING ticket_id
)
INSERT INTO public.guest_tickets (ticket_id, final_calculated_fee, paid_directly)
SELECT ticket_id, 20000.00, true FROM t;

-- ==============================================================================
-- 7. SEED ALERTS
-- ==============================================================================
INSERT INTO public.alerts (type, message, timestamp, acknowledged) VALUES
    ('SECURITY_BREACH', 'User ID 1001 already has an active ticket', '2026-04-22 21:41:27.841', true),
    ('SECURITY_BREACH', 'AAAAAAAAAAAAAAA', '2026-04-25 22:52:39', true),
    ('SYSTEM_FAILURE', 'SHIT NOT WORK', '2026-04-22 16:09:54', false);