package raicod3.example.com.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.dto.email.EmailRequest;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.lib.rabbitmq.RabbitMQProducer;
import raicod3.example.com.model.Job;
import raicod3.example.com.model.Notification;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.payload.ProviderMatchEvent;
import raicod3.example.com.repository.JobRepository;
import raicod3.example.com.repository.NotificationRepository;
import raicod3.example.com.repository.ProviderProfileRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderMatchWorker {

    private final ProviderProfileRepository providerProfileRepository;
    private final NotificationRepository notificationRepository;
    private final RabbitMQProducer rabbitMQProducer;
    private final JobRepository jobRepository;

    @RabbitListener(queues = RabbitMQConfig.PROVIDER_MATCH_QUEUE)
    public void handle(ProviderMatchEvent event) {
        Job job = jobRepository.findByIdWithCategory(event.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + event.jobId()));

        String normalizedCategory = event.category().trim().toLowerCase();

        List<ProviderProfile> matches = providerProfileRepository
                .findByServiceIgnoreCase(normalizedCategory);

        log.info("Found {} providers matching category '{}' for job {}",
                matches.size(), event.category(), event.jobId());

        for (ProviderProfile provider : matches) {
            try {
                notifyOne(provider, job);
            } catch (Exception e) {
                log.error("Failed to notify provider {} for job {}",
                        provider.getId(), event.jobId(), e);
            }
        }
    }

    private void notifyOne(ProviderProfile provider, Job job) {
        Notification n = new Notification();
        n.setRecipient(provider.getUser());
        n.setType("NEW_JOB_MATCH");
        n.setTitle("New job posted: " + job.getCategory().getName());
        n.setMessage(job.getDescription());
        n.setRelatedJobId(job.getId());
        n.setRead(false);
        notificationRepository.save(n);

        String jobLink = "http://localhost:5173/dashboard/leads/" + job.getId();

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setEmail(provider.getUser().getEmail());
        emailRequest.setSubject("New " + job.getCategory().getName() + " job near you");
        emailRequest.setCategory(job.getCategory().getName());
        emailRequest.setDescription(job.getDescription());
        emailRequest.setTemplatePath("/email/new-job-match");
        emailRequest.setJobLink(jobLink);
        rabbitMQProducer.sendEmailNotification(emailRequest);
    }
}