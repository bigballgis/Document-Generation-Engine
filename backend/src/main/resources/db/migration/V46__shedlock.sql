-- LR-B2 (ADR-0044): ShedLock distributed scheduler mutex table.
-- Standard ShedLock JDBC schema; rows are lock leases keyed by job name.
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
