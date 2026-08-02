package raicod3.example.com.dto.chat;

import raicod3.example.com.model.ChatMessage;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatResponseDto(
        UUID id,
        UUID senderId,
        String content,
        LocalDateTime createdAt,
        boolean isRead
) {
    public static ChatResponseDto from(ChatMessage message) {
        return new ChatResponseDto(
                message.getId(),
                message.getSender().getId(),
                message.getContent(),
                message.getCreatedAt(),
                message.isRead()
        );
    }
}
