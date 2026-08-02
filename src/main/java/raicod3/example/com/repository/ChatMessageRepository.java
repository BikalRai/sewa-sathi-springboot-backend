package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raicod3.example.com.model.ChatMessage;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    // Fetch chronological chat history for a specific job
    @Query("SELECT c FROM ChatMessage c WHERE c.job.id = :jobId ORDER BY c.createdAt ASC")
    List<ChatMessage> findByJobIdOrderByCreatedAtAsc(@Param("jobId") UUID jobId);

    // Bulk update to mark all unread messages for a recipient in a specific job as read
    @Modifying
    @Query("UPDATE ChatMessage c SET c.isRead = true WHERE c.job.id = :jobId AND c.recipient.id = :userId AND c.isRead = false")
    int markMessagesAsRead(@Param("jobId") UUID jobId, @Param("userId") UUID userId);

    List<ChatMessage> findBySender_IdOrRecipient_IdOrderByJob_IdAscCreatedAtDesc(UUID senderId, UUID recipientId);
}
