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
-- 1. SSO CHECKOUT AUTOMATION
-- Updated to reference the shared public.tickets table
-- ==============================================================================
CREATE OR REPLACE FUNCTION process_sso_checkout() RETURNS TRIGGER AS $$
DECLARE
    hours_parked numeric;
    ticket_total numeric;
    target_bill_id bigint;
    current_month date;
    t_entry_time timestamptz;
    t_exit_time timestamptz;
    t_price numeric;
BEGIN
    -- Get data from the parent ticket table
    SELECT entry_time, exit_time, price 
    INTO t_entry_time, t_exit_time, t_price
    FROM public.tickets WHERE ticket_id = NEW.ticket_id;

    -- Execute only when a car officially leaves (exit_time is set)
    IF t_exit_time IS NOT NULL THEN
        
        -- Calculate hours
        hours_parked := CEIL(EXTRACT(EPOCH FROM (t_exit_time - t_entry_time)) / 3600.0);
        IF hours_parked < 1 THEN hours_parked := 1; END IF;

        ticket_total := t_price * hours_parked;
        current_month := date_trunc('month', t_exit_time)::date;

        -- Find or Create Bill
        SELECT bill_id INTO target_bill_id FROM public.billing 
        WHERE user_id = NEW.user_id AND billing_month = current_month AND status = 'UNPAID'
        LIMIT 1;

        IF target_bill_id IS NULL THEN
            INSERT INTO public.billing (user_id, billing_month, amount, status)
            VALUES (NEW.user_id, current_month, ticket_total, 'UNPAID')
            RETURNING bill_id INTO target_bill_id;
        ELSE
            UPDATE public.billing SET amount = amount + ticket_total, last_updated = now()
            WHERE bill_id = target_bill_id;
        END IF;

        -- Link bill to sso_ticket
        NEW.bill_id := target_bill_id;

        -- Finalize the parent ticket
        UPDATE public.tickets SET finished = true WHERE ticket_id = NEW.ticket_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger remains on sso_tickets
CREATE TRIGGER sso_ticket_checkout_trigger
BEFORE UPDATE ON public.sso_tickets
FOR EACH ROW EXECUTE FUNCTION process_sso_checkout();

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