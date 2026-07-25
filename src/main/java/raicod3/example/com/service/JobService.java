package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.dto.bid.BidSummaryDto;
import raicod3.example.com.dto.job.JobRequestDto;
import raicod3.example.com.dto.job.JobResponseDto;
import raicod3.example.com.enums.BidStatus;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.model.*;
import raicod3.example.com.payload.JobAnalysisEvent;
import raicod3.example.com.repository.*;
import raicod3.example.com.utilities.LocationUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final CustomerRepository customerRepository;
    private final JobUnlockRepository jobUnlockRepository;
    private final RabbitTemplate rabbitTemplate;
    private final BidService bidService;
    private final BidRepository bidRepository;
    private final ProviderCreditsRepository providerCreditsRepository;
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;

    // Customer post a job
    @Transactional
    @Auditable(action = "CUSTOMER_POST_JOB")
    public JobResponseDto postJob(UUID userId, JobRequestDto dto) {

        // 1. Get customer profile
        CustomerProfile customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        // 2. Get category
        JobCategory category = jobCategoryRepository.findById(UUID.fromString(dto.getCategory()))
                .orElseThrow(() -> new ResourceNotFoundException("Job category not found"));

        // 3. Build Job (This natively pulls latitude, longitude, and address from the DTO)
        Job job = new Job(dto, customer, category);

        // 4. Handle Status
        List<String> images = dto.getImages();
        if (images != null && !images.isEmpty()) {
            job.setStatus(JobStatus.ANALYZING);
        } else {
            job.setStatus(JobStatus.OPEN);
        }

        // 5. Save Job (Location is now safely stored in the `jobs` table)
        Job savedJob = jobRepository.saveAndFlush(job);

        // 6. Publish to RabbitMQ safely AFTER commit
        if (images != null && !images.isEmpty()) {
            JobAnalysisEvent event = new JobAnalysisEvent(savedJob.getId(), customer.getUser().getId().toString(), images, category.getName(),savedJob.getDescription());

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.JOB_ANALYSIS_ROUTING_KEY, event);
                    log.info("Published JobAnalysisEvent to RabbitMQ for Job ID: {}", savedJob.getId());
                }
            });
        }

        return toDto(savedJob, true, true, false, null);
    }

    @Transactional(readOnly = true)
    public List<JobResponseDto> getJobs(UUID userId) {
        CustomerProfile customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        return jobRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                .stream().map(job -> toDto(job, true, true, false, null)).toList();
    }

    @Transactional
    @Auditable(action = "CUSTOMER_CANCEL_JOB")
    public JobResponseDto cancelJob(UUID userId, UUID jobId) {
        Job job = getJobAndValidateOwner(userId, jobId);

        if(job.getStatus() != JobStatus.OPEN) {
            throw new BadRequestException("Only OPEN jobs can be cancelled");
        }

        job.setStatus(JobStatus.CANCELLED);
        return toDto(jobRepository.save(job), true, true, false, null);
    }

    @Transactional(readOnly = true)
    public List<JobResponseDto> getOpenJobs(UUID userId) {
        ProviderProfile provider = providerRepository.findByUserId(userId);

        if (provider == null || provider.getServices() == null || provider.getServices().isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Fetch matched jobs
        List<Job> matchedJobs = jobRepository.findByStatusAndCategoryNameInOrderByCreatedAtDesc(
                JobStatus.OPEN,
                provider.getServices()
        );

        if (matchedJobs.isEmpty()) {
            return Collections.emptyList();
        }

        // --- THE BATCH FETCHING LOGIC ---

        // Extract all Job IDs from the matched jobs
        List<UUID> jobIds = matchedJobs.stream().map(Job::getId).toList();

        // Batch fetch Unlocks and convert to a Set for O(1) lookups
        Set<UUID> unlockedJobIds = jobUnlockRepository.findByProviderIdAndJobIdIn(provider.getId(), jobIds)
                .stream()
                .map(unlock -> unlock.getJob().getId())
                .collect(Collectors.toSet());

        // Batch fetch Bids and convert to a Map (JobId -> Bid) for O(1) lookups
        Map<UUID, Bid> providerBidsMap = bidRepository.findByProviderIdAndJobIdIn(provider.getId(), jobIds)
                .stream()
                .collect(Collectors.toMap(bid -> bid.getJob().getId(), bid -> bid));

        // --- EXTRACT PROVIDER BASE COORDINATES ---
        UserAddress providerAddress = provider.getUser().getUserAddress();
        Double providerLat = providerAddress != null ? providerAddress.getLatitude() : null;
        Double providerLon = providerAddress != null ? providerAddress.getLongitude() : null;

        // 2. Map to DTO in memory
        return matchedJobs.stream().map(job -> {
            // Fast memory lookups instead of database hits
            boolean isUnlocked = unlockedJobIds.contains(job.getId());
            Bid myBid = providerBidsMap.get(job.getId());
            BidSummaryDto bidSummary = myBid != null ? BidSummaryDto.from(myBid, false) : null;

            if (providerLat == null || providerLon == null) {
                // Fallback if provider has no address
                return toDto(job, isUnlocked, false, isUnlocked, bidSummary);
            }

            // Return with dynamically calculated distance and correct UI state flags
            return toDtoWithDistance(
                    job,
                    isUnlocked, // includeAddress (show real address if unlocked)
                    false,      // includeContact (keep hidden in list view)
                    isUnlocked, // isUnlocked
                    bidSummary, // myBid
                    providerLat,
                    providerLon
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<JobResponseDto> getJobsList(UUID userId) {
// 1. Ensure the provider exists
        ProviderProfile provider = providerRepository.findByUserId(userId);
        if (provider == null) {
            return Collections.emptyList();
        }

        // 2. Fetch all relevant bids (Pending + Accepted).
        // The JOIN FETCH in the repository ensures the Job entities are loaded in memory.
        // NOTE: If you strictly want ONLY accepted/completed, remove BidStatus.PENDING from this list.
        List<BidStatus> activeStatuses = List.of(BidStatus.PENDING, BidStatus.ACCEPTED);

        List<Bid> myBids = bidRepository.findByProviderIdAndStatusInWithJobs(
                provider.getId(),
                activeStatuses
        );

        if (myBids.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Extract provider's location for distance mapping
        UserAddress providerAddress = provider.getUser().getUserAddress();
        Double providerLat = providerAddress != null ? providerAddress.getLatitude() : null;
        Double providerLon = providerAddress != null ? providerAddress.getLongitude() : null;

        // 4. Map directly to DTOs in O(N) time with zero extra database hits
        return myBids.stream().map(bid -> {
            Job job = bid.getJob();
            BidSummaryDto bidSummary = BidSummaryDto.from(bid, false);

            // SECURITY CHECK: Only reveal exact address and phone number if the bid was Accepted.
            // A Pending bid means the customer hasn't hired them yet.
            boolean isAccepted = bid.getStatus() == BidStatus.ACCEPTED;

            // Since they placed a bid, we know they unlocked it. No need to query the Unlock table.
            boolean isUnlocked = true;

            return toDtoWithDistance(
                    job,
                    isAccepted,  // includeAddress (True if accepted, false if just pending)
                    isAccepted,  // includeContact (True if accepted, false if just pending)
                    isUnlocked,  // isUnlocked (Always true here)
                    bidSummary,  // myBid
                    providerLat,
                    providerLon
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public JobResponseDto getJob(UUID jobId, UUID userId) { // Assuming controller passes the authenticated User ID
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // 1. Get the provider profile using the User ID
        ProviderProfile provider = providerRepository.findByUserId(userId);
        if (provider == null) {
            throw new UnauthorizedException("Provider profile not found.");
        }

        // 2. Security Checks (using the actual provider.getId())
        boolean isUnlocked = jobUnlockRepository.existsByJob_IdAndProvider_Id(jobId, provider.getId());
        boolean hasWonBid = bidService.isBidAccepted(jobId, provider.getId());

        BidSummaryDto myBid = bidRepository.findByJobIdAndProviderId(jobId, provider.getId())
                .map(bid -> BidSummaryDto.from(bid, false)) // false = don't reveal phone number
                .orElse(null);

        // 3. Distance Calculation Setup
        UserAddress providerAddress = provider.getUser().getUserAddress();

        if (providerAddress == null) {
            // Fallback: If they somehow have no address, return without distance
            return toDto(job, isUnlocked, hasWonBid, isUnlocked, myBid);
        }

        // 4. Return with full masking logic AND distance
        return toDtoWithDistance(
                job,
                isUnlocked,
                hasWonBid,
                isUnlocked,
                myBid,
                providerAddress.getLatitude(),
                providerAddress.getLongitude()
        );
    }

    @Transactional(readOnly = true)
    public JobResponseDto getMyJob(UUID userId, UUID jobId) {
        Job job = getJobAndValidateOwner(userId, jobId);
        return toDto(job, true, true, false, null); // customer always sees their own full address
    }

    private Job getJobAndValidateOwner(UUID userId, UUID jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        boolean isOwner = job.getCustomer().getUser().getId().equals(userId);
        if(!isOwner) {
            throw new UnauthorizedException("You are not allowed to perform this action");
        }
        return job;
    }

    private JobResponseDto toDto(Job job, boolean showFullAddress, boolean showContact, boolean isUnLocked, BidSummaryDto myBid) {
        return JobResponseDto.builder()
                .id(job.getId())
                .description(job.getDescription())
                .categoryName(job.getCategory().getName())
                .categoryIcon(job.getCategory().getIconUrl())
                .urgency(job.getUrgency())
                .status(job.getStatus())
                .difficulty(job.getDifficulty())
                .images(job.getImages() != null ? new java.util.ArrayList<>(job.getImages()) : new java.util.ArrayList<>())
                .latitude(job.getLatitude())
                .longitude(job.getLongitude())

                // --- THE GATEKEEPER LOGIC ---
                // If true, show the real address. If false, show the masked area (e.g., "New Baneshwor").
                .address(showFullAddress ? job.getAddress() : job.getMaskedAddress())

                // If true, show the real number. If false, send null so the UI can't render it.
                .contactNumber(showContact ? job.getContactNumber() : null)
                .isUnlocked(isUnLocked)

                .customerName(job.getCustomer().getUser().getFullName())
                .customerImageUrl(job.getCustomer().getUser().getImageUrl())
                .bidCount(job.getBids() != null ? job.getBids().size() : 0)
                .createdAt(job.getCreatedAt())
                .expiresAt(job.getExpiresAt())
                .myBid(myBid)
                .build();
    }

    private JobResponseDto toDtoWithDistance(Job job, boolean includeAddress, boolean includeContact, boolean isUnlocked, BidSummaryDto myBid, Double providerLat, Double providerLon) {
        JobResponseDto dto = toDto(job, includeAddress, includeContact, isUnlocked, myBid);

        // Calculate distance: Provider Base (UserAddress) -> Job Location (Job entity)
        dto.setDistance(LocationUtils.calculateDistance(
                providerLat, providerLon,
                job.getLatitude(), job.getLongitude()
        ));

        return dto;
    }
}
