package com.querymind.workspace.repository;

import com.querymind.workspace.domain.WorkspaceMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    @Query("select m from WorkspaceMember m where m.workspaceId = :workspaceId and m.userId = :userId")
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    @Query("select m from WorkspaceMember m where m.userId = :userId")
    List<WorkspaceMember> findAllByUserId(UUID userId);
}
