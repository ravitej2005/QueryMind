package com.querymind.ai.repository;

import com.querymind.ai.domain.ChatMessage;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    @Query("select m from ChatMessage m where m.workspaceId = :workspaceId and m.connectionId = :connectionId order by m.createdAt desc")
    Page<ChatMessage> findAllByWorkspaceAndConnection(UUID workspaceId, UUID connectionId, Pageable pageable);
}
