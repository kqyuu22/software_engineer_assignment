-- ==============================================================================
-- 1. SSO CHECKOUT AUTOMATION
-- Calculates hours parked, generates cost, and routes it to a monthly bill.
-- ==============================================================================
CREATE OR REPLACE FUNCTION process_sso_checkout() RETURNS TRIGGER AS $$
DECLARE
    hours_parked numeric;
    ticket_total numeric;
    target_bill_id bigint;
    current_month date;
BEGIN
    -- Execute only when a car officially leaves
    IF NEW.exit_time IS NOT NULL AND OLD.exit_time IS NULL THEN
        
        -- Calculate ceiling hours (minimum 1 hour)
        hours_parked := CEIL(EXTRACT(EPOCH FROM (NEW.exit_time - NEW.entry_time)) / 3600.0);
        IF hours_parked < 1 THEN hours_parked := 1; END IF;

        ticket_total := NEW.price * hours_parked;
        current_month := date_trunc('month', NEW.exit_time)::date;

        -- Find an existing unpaid bill for this user/month
        SELECT bill_id INTO target_bill_id 
        FROM public.billing 
        WHERE user_id = NEW.user_id 
          AND billing_month = current_month 
          AND status = 'UNPAID'
        LIMIT 1;

        -- Route the charge
        IF target_bill_id IS NULL THEN
            -- Generate a new bill
            INSERT INTO public.billing (user_id, billing_month, amount, status)
            VALUES (NEW.user_id, current_month, ticket_total, 'UNPAID')
            RETURNING bill_id INTO target_bill_id;
        ELSE
            -- Accumulate to existing bill
            UPDATE public.billing 
            SET amount = amount + ticket_total,
                last_updated = now()
            WHERE bill_id = target_bill_id;
        END IF;

        -- Link ticket and finalize
        NEW.bill_id := target_bill_id;
        NEW.finished := true;
        
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Bind the automation to the SSO tickets table
CREATE TRIGGER sso_ticket_checkout_trigger
BEFORE UPDATE ON public.sso_tickets
FOR EACH ROW EXECUTE FUNCTION process_sso_checkout();

-- ==============================================================================
-- 2. CROSS-TABLE ACTIVE PLATE VALIDATION
-- Prevents a guest from entering with an SSO's active plate, and vice versa.
-- ==============================================================================

CREATE OR REPLACE FUNCTION check_cross_table_active_plate() RETURNS TRIGGER AS $$
BEGIN
    -- We only care about checking cars that are actively parked (finished = false)
    IF NEW.finished = false THEN
        
        -- If inserting into SSO, check the Guest table
        IF TG_TABLE_NAME = 'sso_tickets' THEN
            IF EXISTS (
                SELECT 1 FROM public.guest_tickets 
                WHERE license_plate = NEW.license_plate AND finished = false
            ) THEN
                RAISE EXCEPTION 'Security Breach: License plate % is already actively parked as a GUEST.', NEW.license_plate;
            END IF;
            
        -- If inserting into Guest, check the SSO table
        ELSIF TG_TABLE_NAME = 'guest_tickets' THEN
            IF EXISTS (
                SELECT 1 FROM public.sso_tickets 
                WHERE license_plate = NEW.license_plate AND finished = false
            ) THEN
                RAISE EXCEPTION 'Security Breach: License plate % is already actively parked as an SSO user.', NEW.license_plate;
            END IF;
        END IF;

    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Bind the radar to both tables
CREATE TRIGGER enforce_sso_plate_cross_check
BEFORE INSERT OR UPDATE ON public.sso_tickets
FOR EACH ROW EXECUTE FUNCTION check_cross_table_active_plate();

CREATE TRIGGER enforce_guest_plate_cross_check
BEFORE INSERT OR UPDATE ON public.guest_tickets
FOR EACH ROW EXECUTE FUNCTION check_cross_table_active_plate();
