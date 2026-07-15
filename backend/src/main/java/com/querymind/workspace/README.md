# workspace module

Responsibility: workspace creation, membership, role (OWNER/EDITOR/VIEWER).

Public service interface: `WorkspaceService` — createWorkspaceForNewUser,
listWorkspacesForUser, resolveRole. Other modules (e.g. connection,
dashboard) must depend on this interface only, never on
WorkspaceRepository/Workspace entity directly.
