package raicod3.example.com.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raicod3.example.com.model.Notification;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(UUID recipientId, Limit limit);

    long countByRecipient_IdAndIsReadFalse(UUID recipientId);
}
