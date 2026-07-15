package com.querymind.workspace.controller;

import com.querymind.workspace.dto.WorkspaceResponse;
import com.querymind.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @Operation(summary = "List workspaces the current user belongs to")
    @GetMapping
    public List<WorkspaceResponse> list(@AuthenticationPrincipal UUID userId) {
        return workspaceService.listWorkspacesForUser(userId);
    }
}
