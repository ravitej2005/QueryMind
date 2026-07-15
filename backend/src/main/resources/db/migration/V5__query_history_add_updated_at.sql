-- V5: Add missing updated_at column to query_history.
--
-- Root cause: V3__query_history.sql omitted the updated_at column that
-- BaseEntity declares, causing Hibernate schema-validation to fail at startup.
-- All other tables (users, workspaces, connections, chat_messages) already
-- include updated_at. This migration brings query_history into line.
--
-- The DEFAULT value back-fills existing rows (if any) with a sensible
-- timestamp equal to created_at so the NOT NULL constraint is satisfied.
-- New rows will always have updated_at set by BaseEntity.onUpdate()/@PrePersist.

ALTER TABLE query_history
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT '1970-01-01 00:00:00.000000'
        AFTER created_at;

-- Back-fill: set updated_at = created_at for any rows already present.
UPDATE query_history SET updated_at = created_at;
