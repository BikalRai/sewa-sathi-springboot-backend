package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.dto.chat.ChatResponseDto;
import raicod3.example.com.dto.chat.ConversationSummaryDto;
import raicod3.example.com.enums.BidStatus;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.model.Bid;
import raicod3.example.com.model.ChatMessage;
import raicod3.example.com.model.Job;
import raicod3.example.com.model.User;
import raicod3.example.com.payload.ChatMessageEvent;
import raicod3.example.com.repository.ChatMessageRepository;
import raicod3.example.com.repository.JobRepository;
import raicod3.example.com.repository.UserRepository;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final JobRepository jobRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void sendMessage(UUID senderId, UUID jobId, String content) {
        // 1. Fetch Job and Sender
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Identify the Accepted Bid
        Bid acceptedBid = job.getBids().stream()
                .filter(b -> b.getStatus() == BidStatus.ACCEPTED)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Cannot chat until a bid is accepted."));

        // 3. Security Check & Determine Recipient
        User recipient;
        boolean isCustomer = job.getCustomer().getUser().getId().equals(senderId);
        boolean isAcceptedProvider = acceptedBid.getProvider().getUser().getId().equals(senderId);

        if (isCustomer) {
            recipient = acceptedBid.getProvider().getUser();
        } else if (isAcceptedProvider) {
            recipient = job.getCustomer().getUser();
        } else {
            throw new UnauthorizedException("You are not authorized to participate in this chat.");
        }

        // 4. Save to Database
        ChatMessage message = new ChatMessage();
        message.setJob(job);
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setRead(false);

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 5. Fire WebSocket Event via RabbitMQ (ONLY after DB commit is successful)
        ChatMessageEvent event = new ChatMessageEvent(
                savedMessage.getId(),
                job.getId(),
                sender.getId().toString(),
                recipient.getId().toString(),
                content,
                savedMessage.getCreatedAt()
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE,
                        RabbitMQConfig.CHAT_ROUTING_KEY,
                        event
                );
                log.info("Published ChatMessageEvent for Job ID: {}", job.getId());
            }
        });
    }

    @Transactional
    public List<ChatResponseDto> getJobChatHistory(UUID userId, UUID jobId) {
        // 1. Verify access (Are they the customer or the accepted provider?)
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        Bid acceptedBid = job.getBids().stream()
                .filter(b -> b.getStatus() == BidStatus.ACCEPTED)
                .findFirst()
                .orElse(null);

        boolean isCustomer = job.getCustomer().getUser().getId().equals(userId);
        boolean isAcceptedProvider = acceptedBid != null && acceptedBid.getProvider().getUser().getId().equals(userId);

        if (!isCustomer && !isAcceptedProvider) {
            throw new UnauthorizedException("You are not authorized to view this chat.");
        }

        // 2. Mark any unread messages sent TO this user as read
        chatMessageRepository.markMessagesAsRead(jobId, userId);

        // 3. Fetch and return chronological history
        return chatMessageRepository.findByJobIdOrderByCreatedAtAsc(jobId)
                .stream()
                .map(ChatResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getInbox(UUID userId) {
        List<ChatMessage> messages = chatMessageRepository
                .findBySender_IdOrRecipient_IdOrderByJob_IdAscCreatedAtDesc(userId, userId);

        // Keep only the latest message per job
        Map<UUID, ChatMessage> latestPerJob = new LinkedHashMap<>();
        for (ChatMessage msg : messages) {
            latestPerJob.putIfAbsent(msg.getJob().getId(), msg);
        }

        // Map to DTO, sorted by most recent activity first
        return latestPerJob.values().stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt).reversed())
                .map(msg -> toConversationSummary(msg, userId))
                .toList();
    }

    private ConversationSummaryDto toConversationSummary(ChatMessage msg, UUID userId) {
        Job job = msg.getJob();
        User otherParty = msg.getSender().getId().equals(userId)
                ? msg.getRecipient()
                : msg.getSender();

        return ConversationSummaryDto.builder()
                .jobId(job.getId())
                .otherPartyName(otherParty.getFullName())
                .lastMessage(msg.getContent())
                .lastMessageAt(msg.getCreatedAt())
                .build();
    }
}
