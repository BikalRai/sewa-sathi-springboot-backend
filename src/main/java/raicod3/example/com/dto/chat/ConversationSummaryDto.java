package raicod3.example.com.dto.chat;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ConversationSummaryDto {
    private UUID jobId;
    private String otherPartyName;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
}
