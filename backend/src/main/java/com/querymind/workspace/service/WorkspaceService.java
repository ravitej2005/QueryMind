package com.querymind.workspace.service;

import com.querymind.workspace.dto.WorkspaceResponse;
import java.util.List;
import java.util.UUID;

/**
 * Public interface for the workspace module. Other modules must depend on
 * this interface only — never on WorkspaceRepository or the Workspace entity
 * directly (module boundary rule, memory.md §2).
 */
public interface WorkspaceService {

    WorkspaceResponse createWorkspaceForNewUser(UUID ownerId, String ownerDisplayName);

    List<WorkspaceResponse> listWorkspacesForUser(UUID userId);

    /**
     * Resolves the caller's role in a workspace per-request (never trust a
     * stale JWT claim for RBAC, memory.md / phases.md Phase1 requirement).
     */
    String resolveRole(UUID workspaceId, UUID userId);
}
