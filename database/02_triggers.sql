DO $$ 
DECLARE 
    r RECORD;
BEGIN
    FOR r IN (SELECT trigger_name, event_object_table 
              FROM information_schema.triggers 
              WHERE trigger_schema = 'public') 
    LOOP
        EXECUTE 'DROP TRIGGER IF EXISTS ' || quote_ident(r.trigger_name) || ' ON ' || quote_ident(r.event_object_table);
    END LOOP;
END $$;

-- ==============================================================================
-- 2. CROSS-TABLE ACTIVE PLATE VALIDATION
-- Now checks the parent tickets table instead of sub-tables
-- ==============================================================================
CREATE OR REPLACE FUNCTION check_active_plate_exists() RETURNS TRIGGER AS $$
BEGIN
    -- Check if this license plate already has an unfinished ticket
    IF EXISTS (
        SELECT 1 FROM public.tickets 
        WHERE license_plate = NEW.license_plate 
        AND finished = false 
        AND ticket_id != NEW.ticket_id
    ) THEN
        RAISE EXCEPTION 'Security Breach: License plate % is already actively parked.', NEW.license_plate;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enforce_plate_check
BEFORE INSERT OR UPDATE ON public.tickets
FOR EACH ROW EXECUTE FUNCTION check_active_plate_exists();

-- ==============================================================================
-- 3.
-- 
-- ==============================================================================
CREATE OR REPLACE FUNCTION get_price_by_slot(input_slot_id integer)
RETURNS numeric(10, 2) AS $$
BEGIN
    RETURN (
        SELECT p.price 
        FROM public.price p
        JOIN public.parking_slots s ON p.slot_priority = s.priority
        WHERE s.slot_id = input_slot_id
    );
END;
$$ LANGUAGE plpgsql;

-- Trigger Function
CREATE OR REPLACE FUNCTION trg_set_ticket_price()
RETURNS TRIGGER AS $$
BEGIN
    -- Only fetch price if slot_id is provided
    IF NEW.parking_spot IS NOT NULL THEN
        NEW.price := get_price_by_slot(NEW.parking_spot);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply Trigger to Table
CREATE TRIGGER t_populate_price
BEFORE INSERT ON public.tickets
FOR EACH ROW
EXECUTE FUNCTION trg_set_ticket_price();


-- Ensure no same user is active in more than two tickets
CREATE OR REPLACE FUNCTION check_active_sso_user() RETURNS TRIGGER AS $$
BEGIN
    -- Check if this user already has an unfinished ticket in the system
    IF EXISTS (
        SELECT 1 
        FROM public.sso_tickets st
        JOIN public.tickets t ON st.ticket_id = t.ticket_id
        WHERE st.user_id = NEW.user_id 
        AND t.finished = false
    ) THEN
        RAISE EXCEPTION 'User % already has an active, unfinished parking ticket.', NEW.user_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enforce_single_active_sso_ticket
BEFORE INSERT ON public.sso_tickets
FOR EACH ROW 
EXECUTE FUNCTION check_active_sso_user();