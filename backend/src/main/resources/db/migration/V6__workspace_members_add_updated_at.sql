-- V6: Add missing updated_at column to workspace_members.
--
-- Root cause: V1__init_auth_workspace.sql included created_at on workspace_members
-- but omitted updated_at. WorkspaceMember extends BaseEntity which declares both
-- createdAt and updatedAt, causing Hibernate schema-validation to fail at startup.
--
-- users and workspaces in the same V1 migration correctly include updated_at;
-- this was a localised omission on the workspace_members table only.

ALTER TABLE workspace_members
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT '1970-01-01 00:00:00.000000'
        AFTER created_at;

-- Back-fill: set updated_at = created_at for any rows already present.
UPDATE workspace_members SET updated_at = created_at;
