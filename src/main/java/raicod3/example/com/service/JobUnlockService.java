package raicod3.example.com.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.dto.job.JobUnlockResponseDto;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.Job;
import raicod3.example.com.model.JobUnlock;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.repository.JobRepository;
import raicod3.example.com.repository.JobUnlockRepository;
import raicod3.example.com.repository.ProviderCreditsRepository;
import raicod3.example.com.repository.ProviderProfileRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class JobUnlockService {
    private static final int UNLOCK_COST = 1;
    private static final int MAX_UNLOCKS = 3;

    private final ProviderProfileRepository providerProfileRepository;
    private final JobRepository jobRepository;
    private final JobUnlockRepository jobUnlockRepository;
    private final ProviderCreditsRepository providerCreditsRepository;

    @Auditable(action = "PROVIDER_UNLOCKED_JOB")
    @Transactional
    public JobUnlockResponseDto unlockJob(UUID userId, UUID jobId) {
        log.debug("Validating provider...");
        ProviderProfile provider = providerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        log.debug("Unlocking job with id {}", jobId);

        // 1. Idempotency check - cheapest check, do this before locking anything
        if(jobUnlockRepository.existsByJob_IdAndProvider_Id(jobId, provider.getId())) {
            throw new BadRequestException("You have already unlocked this job.");
        }

        // 2. Acquire Pessimistic Write Lock on the Job
        // Any other threads trying to unlock this exact job will PAUSE on this line
        // until the current transaction completes.
        Job job = jobRepository.findByIdWithWriteLock(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found."));

        // 3. Verify Capacity
        // Because of the lock, we are guaranteed this count is perfectly accurate
        int currentUnlocks = jobUnlockRepository.countByJob_Id(jobId);
        if (currentUnlocks >= MAX_UNLOCKS) {
            throw new BadRequestException("This job has reached the maximum number of interested providers.");
        }

        // 4. Atomic credit deduction
        int rowsUpdated = providerCreditsRepository.deductCredits(provider.getId(), UNLOCK_COST);
        if(rowsUpdated == 0) {
            throw new BadRequestException("Insufficient credits to unlock this job.");
        }

        // 5. Record the unlock
        JobUnlock unlock = new JobUnlock();
        unlock.setJob(job);
        unlock.setProvider(provider);
        unlock.setTokensSpent(UNLOCK_COST);

        JobUnlock result = jobUnlockRepository.save(unlock);
        log.info("Job unlocked successfully. Total unlocks for job {}: {}", jobId, currentUnlocks + 1);

        return JobUnlockResponseDto.from(result);
    }

    public List<JobUnlockResponseDto> unlockedobs(UUID userId) {
        log.debug("Validating provider...");
        ProviderProfile provider = providerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        List<JobUnlock> unlocks = jobUnlockRepository.findAllByProviderId(provider.getId());

        return unlocks.stream().map(JobUnlockResponseDto::from).toList();
    }

}
