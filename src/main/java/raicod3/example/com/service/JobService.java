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
import raicod3.example.com.dto.job.CompleteJobRequestDto;
import raicod3.example.com.dto.job.JobAnalysisDto;
import raicod3.example.com.dto.job.JobRequestDto;
import raicod3.example.com.dto.job.JobResponseDto;
import raicod3.example.com.dto.provider.ProviderStatsDto;
import raicod3.example.com.dto.rating.RatingResponseDto;
import raicod3.example.com.enums.BidStatus;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.model.*;
import raicod3.example.com.payload.JobAnalysisEvent;
import raicod3.example.com.payload.JobMatchEvent;
import raicod3.example.com.repository.*;
import raicod3.example.com.utilities.LocationUtils;

import java.time.LocalDateTime;
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
    private final RatingRepository ratingRepository;
    private final ProviderRepository providerRepository;
    private final NotificationRepository notificationRepository;

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

        // 5. Save Job
        Job savedJob = jobRepository.saveAndFlush(job);

        // 6. Publish to RabbitMQ safely AFTER commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (images != null && !images.isEmpty()) {
                    // Send for AI processing
                    JobAnalysisEvent event = new JobAnalysisEvent(savedJob.getId(), customer.getUser().getId().toString(), images, category.getName(), savedJob.getDescription());
                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.JOB_ANALYSIS_ROUTING_KEY, event);
                    log.info("Published JobAnalysisEvent for Job ID: {}", savedJob.getId());
                } else {
                    // Instantly trigger matchmaking since it's an OPEN job
                    JobMatchEvent event = new JobMatchEvent(savedJob.getId());
                    rabbitTemplate.convertAndSend(RabbitMQConfig.PROVIDER_MATCH_EXCHANGE, RabbitMQConfig.PROVIDER_MATCH_ROUTING_KEY, event);
                    log.info("Published JobMatchEvent for Job ID: {}", savedJob.getId());
                }
            }
        });

        return toDto(savedJob, true, true, false, null);
    }

    @Transactional(readOnly = true)
    public List<JobResponseDto> getAllJobs() {
        List<Job> jobs = jobRepository.findAllWithDetails();

        return  jobs.stream().map(job -> toDto(job, true, true, false, null)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JobResponseDto> getJobs(UUID userId) {
        CustomerProfile customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        List<Job> jobs = jobRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId());

        List<UUID> completedJobIds = jobs.stream()
                .filter(j -> j.getStatus() == JobStatus.COMPLETED)
                .map(Job::getId)
                .toList();

        Map<UUID, RatingResponseDto> ratingsByJobId = completedJobIds.isEmpty()
                ? Collections.emptyMap()
                : ratingRepository.findByJob_IdIn(completedJobIds).stream()
                .collect(Collectors.toMap(r -> r.getJob().getId(), RatingResponseDto::from));

        return jobs.stream().map(job -> {
            JobResponseDto dto = toDto(job, true, true, false, null);
            dto.setRating(ratingsByJobId.get(job.getId()));
            return dto;
        }).toList();
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
    public ProviderStatsDto getProviderStats(UUID userId) {
        ProviderProfile provider = providerRepository.findByUserId(userId);
        if (provider == null) {
            throw new UnauthorizedException("Provider profile not found.");
        }

        List<Bid> acceptedBids = bidRepository.findByProviderIdAndStatusInWithJobs(
                provider.getId(), List.of(BidStatus.ACCEPTED)
        );

        List<Bid> completedBids = acceptedBids.stream()
                .filter(b -> b.getJob().getStatus() == JobStatus.COMPLETED)
                .toList();

        int totalEarned = completedBids.stream()
                .mapToInt(Bid::getQuotedPrice)
                .sum();

        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        int thisMonthEarned = completedBids.stream()
                .filter(b -> b.getJob().getCompletedAt() != null && b.getJob().getCompletedAt().isAfter(monthStart))
                .mapToInt(Bid::getQuotedPrice)
                .sum();

        long activeJobs = acceptedBids.stream()
                .filter(b -> b.getJob().getStatus() != JobStatus.COMPLETED
                        && b.getJob().getStatus() != JobStatus.CANCELLED)
                .count();

        Double avgRating = ratingRepository.findAverageScoreByProviderId(provider.getId());
        long ratingCount = ratingRepository.countByProvider_Id(provider.getId());

        return new ProviderStatsDto(totalEarned, thisMonthEarned, activeJobs, avgRating, ratingCount);
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

        JobResponseDto dto = toDto(job, true, true, false, null);

        if (job.getStatus() == JobStatus.COMPLETED) {
            RatingResponseDto rating = ratingRepository.findByJob_Id(jobId)
                    .map(RatingResponseDto::from)
                    .orElse(null);
            dto.setRating(rating);
        }

        return dto;
    }

    @Transactional
    @Auditable(action = "PROVIDER_COMPLETE_JOB")
    public JobResponseDto completeJob(UUID userId, UUID jobId, CompleteJobRequestDto dto) {

        ProviderProfile provider = providerRepository.findByUserId(userId);
        if (provider == null) {
            throw new UnauthorizedException("Provider profile not found.");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new BadRequestException("Job cannot be completed from its current state: " + job.getStatus());
        }

        Bid winningBid = bidRepository.findByJobIdAndProviderId(jobId, provider.getId())
                .orElseThrow(() -> new UnauthorizedException("You have not placed a bid on this job."));

        if (winningBid.getStatus() != BidStatus.ACCEPTED) {
            throw new UnauthorizedException("You cannot complete a job you were not hired for.");
        }

        job.setStatus(JobStatus.AWAITING_CONFIRMATION);
        job.setCompletionNotes(dto.getCompletionNotes());
        if (dto.getCompletionImages() != null && !dto.getCompletionImages().isEmpty()) {
            job.setCompletionImages(dto.getCompletionImages());
        }

        Job savedJob = jobRepository.save(job);

        // NEW: notify customer that confirmation is needed
        Notification n = new Notification();
        n.setRecipient(job.getCustomer().getUser());
        n.setType("JOB_AWAITING_CONFIRMATION");
        n.setTitle("Job marked complete");
        n.setMessage("Your provider has marked the " + job.getCategory().getName() + " job as complete. Please confirm.");
        n.setRelatedJobId(job.getId());
        n.setRead(false);
        notificationRepository.save(n);

        return toDto(savedJob, true, true, true, BidSummaryDto.from(winningBid, true));
    }

    @Transactional
    @Auditable(action = "CUSTOMER_CONFIRM_COMPLETION")
    public JobResponseDto confirmJobCompletion(UUID userId, UUID jobId) {

        CustomerProfile customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException("Customer profile not found."));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getCustomer().getId().equals(customer.getId())) {
            throw new UnauthorizedException("This job does not belong to you.");
        }

        if (job.getStatus() != JobStatus.AWAITING_CONFIRMATION) {
            throw new BadRequestException("Job cannot be confirmed from its current state: " + job.getStatus());
        }

        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());

        Job savedJob = jobRepository.save(job);

        Bid winningBid = bidRepository.findByJobIdAndProviderId(jobId, job.getBids().stream()
                        .filter(b -> b.getStatus() == BidStatus.ACCEPTED)
                        .findFirst()
                        .map(b -> b.getProvider().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("No accepted bid found")))
                .orElseThrow(() -> new ResourceNotFoundException("No accepted bid found"));

        return toDto(savedJob, true, true, true, BidSummaryDto.from(winningBid, true));
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
                .address(showFullAddress ? job.getAddress() : job.getMaskedAddress())
                .contactNumber(showContact ? job.getContactNumber() : null)
                .isUnlocked(isUnLocked)

                .customerName(job.getCustomer().getUser().getFullName())
                .customerImageUrl(job.getCustomer().getUser().getImageUrl())
                .bidCount(job.getBids() != null ? job.getBids().size() : 0)
                .createdAt(job.getCreatedAt())
                .expiresAt(job.getExpiresAt())
                .myBid(myBid)
                .unlockCount(job.getUnlockCount() != null ? job.getUnlockCount() : 0)

                // --- THE NEW AI ANALYSIS MAPPING ---
                .aiAnalysis(job.getJobAnalysis() != null ? new JobAnalysisDto(job.getJobAnalysis()) : null)

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
