package com.querymind.connection.repository;

import com.querymind.connection.domain.DatabaseConnection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConnectionRepository extends JpaRepository<DatabaseConnection, UUID> {

    @Query("select c from DatabaseConnection c where c.workspaceId = :workspaceId")
    List<DatabaseConnection> findAllByWorkspaceId(UUID workspaceId);

    @Query("select c from DatabaseConnection c where c.id = :id and c.workspaceId = :workspaceId")
    Optional<DatabaseConnection> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
