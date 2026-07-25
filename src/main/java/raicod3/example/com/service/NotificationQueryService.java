package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.dto.notification.NotificationResponseDto;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.Notification;
import raicod3.example.com.repository.NotificationRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getRecent(UUID userId) {
        return notificationRepository
                .findByRecipient_IdOrderByCreatedAtDesc(userId, Limit.of(30))
                .stream()
                .map(NotificationResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipient_IdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!n.getRecipient().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found"); // don't leak existence to non-owners
        }

        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository
                .findByRecipient_IdOrderByCreatedAtDesc(userId, Limit.of(1000))
                .stream()
                .filter(n -> !n.isRead())
                .toList();

        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
