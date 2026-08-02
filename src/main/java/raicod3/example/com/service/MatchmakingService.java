package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.model.Job;
import raicod3.example.com.model.Notification;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.payload.JobMatchEvent;
import raicod3.example.com.repository.JobRepository;
import raicod3.example.com.repository.NotificationRepository;
import raicod3.example.com.repository.ProviderRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final ProviderRepository providerRepository;
    private final JobRepository jobRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final Double DEFAULT_RADIUS_KM = 5.0;

    @RabbitListener(queues = RabbitMQConfig.PROVIDER_MATCH_QUEUE)
    @Transactional // Note: Removed readOnly=true because we are now saving Notifications to DB
    public void handleJobMatch(JobMatchEvent event) {
        log.info("Starting matchmaking for Job ID: {}", event.jobId());

        Job job = jobRepository.findByIdWithCategory(event.jobId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + event.jobId()));

        if (job.getLatitude() == null || job.getLongitude() == null) {
            log.error("Job lacks coordinates. Aborting match.");
            return;
        }

        List<ProviderProfile> matchedProviders = providerRepository.findProvidersWithinRadius(
                job.getLatitude(), job.getLongitude(), job.getCategory().getName(), DEFAULT_RADIUS_KM
        );

        if (matchedProviders.isEmpty()) {
            return;
        }

        for (ProviderProfile provider : matchedProviders) {
            // 1. Save to Database (so it appears in their in-app notification bell)
            Notification n = new Notification();
            n.setRecipient(provider.getUser());
            n.setType("NEW_LOCAL_LEAD");
            n.setTitle("New Job in your area!");
            n.setMessage("A new " + job.getCategory().getName() + " job is available within 5km.");
            n.setRelatedJobId(job.getId());
            n.setRead(false);
            notificationRepository.save(n);

            // 2. Push Real-Time WebSocket Alert
            // Payload can be whatever your frontend expects for notifications
            messagingTemplate.convertAndSendToUser(
                    provider.getUser().getId().toString(),
                    "/queue/notifications",
                    "NEW_LEAD:" + job.getId()
            );
        }
        log.info("Successfully notified {} providers within 5km for Job ID: {}", matchedProviders.size(), job.getId());
    }
}