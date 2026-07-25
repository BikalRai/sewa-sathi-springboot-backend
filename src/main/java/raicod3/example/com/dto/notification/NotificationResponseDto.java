package raicod3.example.com.dto.notification;

import lombok.Builder;
import lombok.Getter;
import raicod3.example.com.model.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponseDto {
    private UUID id;
    private String type;
    private String title;
    private String message;
    private UUID relatedJobId;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification notification) {
        return  NotificationResponseDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedJobId(notification.getRelatedJobId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
