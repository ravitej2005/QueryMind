package com.querymind.workspace.service;

import com.querymind.common.exception.ApiException;
import com.querymind.workspace.domain.Workspace;
import com.querymind.workspace.domain.WorkspaceMember;
import com.querymind.workspace.domain.WorkspaceRole;
import com.querymind.workspace.dto.WorkspaceResponse;
import com.querymind.workspace.repository.WorkspaceMemberRepository;
import com.querymind.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WorkspaceServiceImpl(
            WorkspaceRepository workspaceRepository, WorkspaceMemberRepository memberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public WorkspaceResponse createWorkspaceForNewUser(UUID ownerId, String ownerDisplayName) {
        Workspace workspace = workspaceRepository.save(
                new Workspace(ownerDisplayName + "'s Workspace", ownerId));
        memberRepository.save(new WorkspaceMember(workspace.getId(), ownerId, WorkspaceRole.OWNER));
        return new WorkspaceResponse(
                workspace.getId(), workspace.getName(), ownerId, WorkspaceRole.OWNER.name());
    }

    @Override
    @Transactional
    public List<WorkspaceResponse> listWorkspacesForUser(UUID userId) {
        return memberRepository.findAllByUserId(userId).stream()
                .map(member -> {
                    Workspace workspace = workspaceRepository.findById(member.getWorkspaceId())
                            .orElseThrow(() -> new ApiException(
                                    HttpStatus.NOT_FOUND, "WORKSPACE_NOT_FOUND", "Workspace not found"));
                    return new WorkspaceResponse(
                            workspace.getId(), workspace.getName(), workspace.getOwnerId(),
                            member.getRole().name());
                })
                .toList();
    }

    @Override
    @Transactional
    public String resolveRole(UUID workspaceId, UUID userId) {
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(m -> m.getRole().name())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.FORBIDDEN, "NOT_A_MEMBER", "You are not a member of this workspace"));
    }
}
