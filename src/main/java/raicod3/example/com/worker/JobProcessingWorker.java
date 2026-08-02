package raicod3.example.com.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.payload.AiJobAnalysis;
import raicod3.example.com.payload.JobAnalysisEvent;
import raicod3.example.com.payload.ProviderMatchEvent;
import raicod3.example.com.service.JobProcessingNotifier;
import tools.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;

    public JobProcessingWorker(JobProcessingNotifier notifier, AmqpTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.notifier = notifier;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;

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
            // Note: If you want to analyze ALL images, you'd loop here and pass a List of base64 strings to Ollama.
            String imageUrl = event.imageUrls().get(0);
            byte[] imageBytes;

            try (InputStream in = new URI(imageUrl).toURL().openStream()) {
                imageBytes = in.readAllBytes();
            }

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Get the rich structured analysis
            AiJobAnalysis analysis = analyzeImageWithGemma(base64Image, event.description());

            log.info("AI Analysis completed. Difficulty: {}, Reasoning: {}, Est Hours: {}, Tools: {}",
                    analysis.difficulty(), analysis.reasoning(), analysis.estimatedHours(), analysis.recommendedTools());

            // Right now you only notify with difficulty, but you can update this signature later to pass the reasoning too!
            notifier.notifyUserJobActive(event.userId(), event.jobId(), analysis);

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

    private AiJobAnalysis analyzeImageWithGemma(String base64Image, String description) {
        log.info("Base64 Image length is: {} characters", base64Image.length());

        // 1. Give the AI a strict role and a strict JSON schema
        String prompt = """
            You are an expert home service estimator.
            
            Customer description: "%s"

            Analyze the attached image and the customer's description. 
            Assess the difficulty, estimate the time required, and list the tools needed.
            
            You MUST respond ONLY with valid JSON using this exact schema:
            {
              "difficulty": "LOW", // Strictly use LOW, MEDIUM, or HIGH
              "reasoning": "Brief explanation of why you chose this difficulty.",
              "estimatedHours": 1.5, // Numeric estimate
              "recommendedTools": ["tool 1", "tool 2"]
            }
            """.formatted(description != null && !description.isBlank() ? description : "No description provided.");

        // 2. Add "format": "json" to the Ollama payload
        Map<String, Object> requestPayload = Map.of(
                "model", "llava",
                "prompt", prompt,
                "images", List.of(base64Image),
                "format", "json", // Forces Ollama to output valid JSON
                "stream", false
        );

        log.info("Executing POST request to Ollama...");

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/api/generate")
                .body(requestPayload)
                .retrieve()
                .body(Map.class);

        if (response != null && response.containsKey("response")) {
            String aiResponseStr = String.valueOf(response.get("response"));

            try {
                // 3. Parse the JSON string directly into our Java Record
                return objectMapper.readValue(aiResponseStr, AiJobAnalysis.class);
            } catch (Exception e) {
                log.error("Failed to parse AI JSON response: {}", aiResponseStr);
                throw new RuntimeException("AI returned malformed JSON", e);
            }
        }

        throw new RuntimeException("Invalid response from Ollama API");
    }
}