package com.querymind.query.repository;

import com.querymind.query.domain.QueryHistory;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QueryHistoryRepository extends JpaRepository<QueryHistory, UUID> {

    @Query("select h from QueryHistory h where h.workspaceId = :workspaceId order by h.createdAt desc")
    Page<QueryHistory> findAllByWorkspaceId(UUID workspaceId, Pageable pageable);
}
