-- ==============================================================================
-- PARTIAL UNIQUE INDEXES
-- Prevents the same license plate from having more than one ACTIVE ticket,
-- while allowing them to have infinite FINISHED tickets in their history.
-- ==============================================================================

CREATE UNIQUE INDEX sso_active_plate_idx 
ON public.sso_tickets (license_plate) 
WHERE finished = false;

CREATE UNIQUE INDEX guest_active_plate_idx 
ON public.guest_tickets (license_plate) 
WHERE finished = false;
