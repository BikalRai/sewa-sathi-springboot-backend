package raicod3.example.com.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.payload.JobAnalysisEvent;
import raicod3.example.com.payload.ProviderMatchEvent;
import raicod3.example.com.service.JobProcessingNotifier;

import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class JobProcessingWorker {
    private final RestClient restClient;
    private final JobProcessingNotifier notifier;
    private final AmqpTemplate rabbitTemplate;

    public JobProcessingWorker(JobProcessingNotifier notifier, AmqpTemplate rabbitTemplate) {
        this.notifier = notifier;
        this.rabbitTemplate = rabbitTemplate;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(180000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("http://localhost:11434")
                .build();
    }

    @RabbitListener(queues = RabbitMQConfig.JOB_ANALYSIS_QUEUE)
    public void processJobImage(JobAnalysisEvent event) {
        log.info("Started processing image for Job ID: {}", event.jobId());

        try {
            String imageUrl = event.imageUrls().get(0);
            byte[] imageBytes;

            try (InputStream in = new URI(imageUrl).toURL().openStream()) {
                imageBytes = in.readAllBytes();
            }

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String difficulty = analyzeImageWithGemma(base64Image, event.description());

            log.info("AI Analysis completed successfully for Job ID: {}. Resulting Difficulty: {}", event.jobId(), difficulty);

            notifier.notifyUserJobActive(event.userId(), event.jobId(), difficulty);

            // Fire-and-forget: provider matching/notification is fully decoupled.
            // If this publish fails, it must NEVER roll back the job-active update above —
            // that's why it's outside notifier's @Transactional method and wrapped separately.
            publishProviderMatchEvent(event);

        } catch (Exception e) {
            log.error("Failed to process image for Job {}", event.jobId(), e);
            notifier.notifyUserJobFailed(event.userId(), event.jobId(), "AI processing failed. Please set difficulty manually.");
        }
    }

    private void publishProviderMatchEvent(JobAnalysisEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PROVIDER_MATCH_EXCHANGE,
                    RabbitMQConfig.PROVIDER_MATCH_ROUTING_KEY,
                    new ProviderMatchEvent(event.jobId(), event.category(), null)
            );
        } catch (Exception e) {
            log.error("Failed to publish ProviderMatchEvent for Job {}", event.jobId(), e);
        }
    }

    private String analyzeImageWithGemma(String base64Image, String description) {
        log.info("Base64 Image length is: {} characters", base64Image.length());

        if (base64Image.length() > 5_000_000) {
            log.warn("WARNING: This image is extremely large. The HTTP request might choke!");
        }

        String prompt = """
            You are assessing the difficulty of a home service job for a service marketplace.

            Job description from the customer: "%s"

            Analyze the attached image together with this description and determine the difficulty
            of completing this job. Consider factors like visible damage severity, access constraints,
            and complexity implied by the description.

            Reply with ONLY one word: LOW, MEDIUM, or HIGH.
            """.formatted(description != null && !description.isBlank() ? description : "No description provided.");

        Map<String, Object> requestPayload = Map.of(
                "model", "llava",
                "prompt", prompt,
                "images", List.of(base64Image),
                "stream", false
        );

        log.info("Executing POST request to Ollama at http://localhost:11434/api/generate...");

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/api/generate")
                .body(requestPayload)
                .retrieve()
                .body(Map.class);

        log.info("Successfully received HTTP response from Ollama API!");

        if (response != null && response.containsKey("response")) {
            String aiResponse = String.valueOf(response.get("response"));
            return aiResponse.trim().toUpperCase();
        }

        throw new RuntimeException("Invalid response from Ollama API");
    }
}