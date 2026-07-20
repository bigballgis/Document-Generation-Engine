-- PD-6: persist production re-issue accountability reason on regeneration rows.
ALTER TABLE invocation_regeneration
    ADD COLUMN reason VARCHAR(500) NULL;
