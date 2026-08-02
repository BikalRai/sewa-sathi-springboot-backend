package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.enums.JobDifficulty;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.Job;
import raicod3.example.com.model.JobAnalysis;
import raicod3.example.com.payload.AiJobAnalysis;
import raicod3.example.com.payload.JobStatusPayload;
import raicod3.example.com.repository.JobRepository;
import raicod3.example.com.repository.JobAnalysisRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobProcessingNotifier {

    private final JobRepository jobRepository;
    private final JobAnalysisRepository jobAnalysisRepository; // 1. Inject new repo
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void notifyUserJobActive(String userId, UUID jobId, AiJobAnalysis aiAnalysis) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        // Safely parse difficulty
        String difficultyStr = aiAnalysis.difficulty() != null ? aiAnalysis.difficulty().toUpperCase() : "MEDIUM";
        try {
            job.setDifficulty(JobDifficulty.valueOf(difficultyStr));
        } catch (Exception e) {
            job.setDifficulty(JobDifficulty.MEDIUM);
        }

        job.setStatus(JobStatus.OPEN); // Setting to OPEN as per your logic

        // 3. Build the rich analysis entity and link it to the job
        JobAnalysis analysisEntity = JobAnalysis.builder()
                .job(job)
                .difficulty(job.getDifficulty().name()) // Use the sanitized enum value
                .reasoning(aiAnalysis.reasoning())
                .estimatedHours(aiAnalysis.estimatedHours())
                .recommendedTools(aiAnalysis.recommendedTools())
                .build();

        // 4. Save both (If cascade=ALL is set on Job.jobAnalysis, saving Job saves both.
        // Otherwise, save explicitly to be safe)
        job.setJobAnalysis(analysisEntity);
        jobAnalysisRepository.save(analysisEntity);
        jobRepository.save(job);

        // 5. Real-Time Update: Push to WebSocket
        // We still just send the difficulty string to the frontend for now
        JobStatusPayload payload = new JobStatusPayload(jobId, "OPEN", job.getDifficulty().name(), null);
        messagingTemplate.convertAndSendToUser(userId, "/queue/job-updates", payload);

        log.info("Job {} updated to OPEN with rich AI analysis, notified user {}", jobId, userId);
    }

    @Transactional
    public void notifyUserJobFailed(String userId, UUID jobId, String reason) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        job.setStatus(JobStatus.FAILED);
        jobRepository.save(job);

        JobStatusPayload payload = new JobStatusPayload(jobId, "FAILED", null, reason);
        messagingTemplate.convertAndSendToUser(userId, "/queue/job-updates", payload);

        log.error("Job {} failed: {}", jobId, reason);
    }
}