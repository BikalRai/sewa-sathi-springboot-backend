package raicod3.example.com.payload;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageEvent(
        UUID messageId,
        UUID jobId,
        String senderId,
        String recipientId,
        String content,
        LocalDateTime timestamp
) {
}
