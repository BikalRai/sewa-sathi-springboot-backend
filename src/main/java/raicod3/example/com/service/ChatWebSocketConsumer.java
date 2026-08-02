package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.payload.ChatMessageEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWebSocketConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE)
    public void consumeChatMessage(ChatMessageEvent event) {
        log.info("Pushing chat message via WebSocket to User: {}", event.recipientId());

        // This pushes to the specific user's private WebSocket channel
        // Frontend should subscribe to: /user/{userId}/queue/messages
        messagingTemplate.convertAndSendToUser(
                event.recipientId(),
                "/queue/messages",
                event
        );
    }

}
