-- ==============================================================================
-- PARTIAL UNIQUE INDEXES
-- Prevents the same license plate from having more than one ACTIVE ticket,
-- while allowing them to have infinite FINISHED tickets in their history.
-- ==============================================================================
CREATE UNIQUE INDEX active_plate_idx 
ON public.tickets (license_plate) 
WHERE finished = false;