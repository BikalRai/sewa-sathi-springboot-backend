package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.dto.bid.BidConfirmationDto;
import raicod3.example.com.dto.bid.BidRequestDto;
import raicod3.example.com.dto.bid.BidSummaryDto;
import raicod3.example.com.enums.BidStatus;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.model.Bid;
import raicod3.example.com.model.Job;
import raicod3.example.com.model.ProviderCredits;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BidService {
    private final BidRepository bidRepository;
    private final JobRepository jobRepository;
    private final ProviderProfileRepository providerProfileRepository;

    // 1. Inject the credits repository
    private final ProviderCreditsRepository providerCreditsRepository;

    // Provider place a bid
    @Transactional
    @Auditable(action = "PROVIDER_PLACE_BID")
    public BidConfirmationDto placeBid(UUID userId, UUID jobId, BidRequestDto dto) {
        // ... (Your existing placeBid logic remains exactly the same)
        ProviderProfile provider = providerProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));

        if(!provider.getIsVerified()) {
            throw new BadRequestException("Your account is pending verification. You cannot bid yet.");
        }

        if(!provider.getIsActive()) {
            throw new BadRequestException("Your account is suspended.");
        }

        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if(job.getStatus() != JobStatus.OPEN) {
            throw new BadRequestException("This job is no longer accepting bids.");
        }

        if (job.getExpiresAt() != null && job.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This job has expired");
        }

        boolean isOwnJob = job.getCustomer().getUser().getId().equals(userId);
        if (isOwnJob) {
            throw new BadRequestException("You cannot bid on your own job");
        }

        if (bidRepository.existsByJobIdAndProviderId(jobId, provider.getId())) {
            throw new BadRequestException("You have already placed a bid on this job");
        }

        int activeBidCount = bidRepository.countActiveBids(jobId);
        if (activeBidCount >= 3) {
            throw new BadRequestException("This job already has the maximum number of bids");
        }

        Bid bid = new Bid();
        bid.setJob(job);
        bid.setProvider(provider);
        bid.setMessage(dto.getMessage());
        bid.setQuotedPrice(dto.getQuotedPrice());

        return BidConfirmationDto.from(bidRepository.save(bid));
    }

    // Customer get bids for job
    @Transactional(readOnly = true)
    public List<BidSummaryDto> getBidsForJob(UUID userId, UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getCustomer().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have access to this job's bids");
        }

        List<Bid> bids = bidRepository.findByJobIdOrderByCreatedAtAsc(jobId);

        if (bids.isEmpty()) {
            return List.of();
        }

        // 2. BATCH FETCH LOGIC: Extract IDs and query tiers all at once
        List<UUID> providerIds = bids.stream()
                .map(bid -> bid.getProvider().getId())
                .toList();

        Map<UUID, String> providerTiers = providerCreditsRepository.findAllById(providerIds)
                .stream()
                .collect(Collectors.toMap(
                        ProviderCredits::getProviderId,
                        credits -> credits.getActiveTier() != null ? credits.getActiveTier().name() : "FREE"
                ));

        // 3. Map to DTO injecting the memory lookup
        return bids.stream()
                .map(bid -> {
                    String tier = providerTiers.getOrDefault(bid.getProvider().getId(), "FREE");
                    return BidSummaryDto.from(bid, false, tier);
                })
                .toList();
    }

    // Customer accept bid
    @Transactional
    @Auditable(action = "CUSTOMER_ACCEPT_BID")
    public BidSummaryDto acceptBid(UUID userId, UUID jobId, UUID bidId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getCustomer().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You don't own this job");
        }

        if (job.getStatus() != JobStatus.OPEN) {
            throw new BadRequestException("This job is not open for acceptance");
        }

        Bid winningBid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found"));

        if (!winningBid.getJob().getId().equals(jobId)) {
            throw new BadRequestException("Bid does not belong to this job");
        }

        if (winningBid.getStatus() != BidStatus.PENDING) {
            throw new BadRequestException("This bid is no longer available.");
        }

        winningBid.setStatus(BidStatus.ACCEPTED);

        bidRepository.findByJobIdAndStatusNot(jobId, BidStatus.WITHDRAWN)
                .stream()
                .filter(b -> !b.getId().equals(bidId))
                .forEach(b -> b.setStatus(BidStatus.REJECTED));

        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.save(job);

        // 4. Inject tier for the single accepted bid
        String activeTier = getActiveTierForProvider(winningBid.getProvider().getId());
        return BidSummaryDto.from(bidRepository.save(winningBid), true, activeTier);
    }

    // Provider withdraw their bid
    @Transactional
    @Auditable(action = "PROVIDER_WITHDRAW_BID")
    public BidConfirmationDto withdrawBid(UUID userId, UUID bidId) {
        // ... (Your existing withdrawBid logic remains exactly the same)
        ProviderProfile provider = providerProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));

        Bid bid = bidRepository.findByIdAndProviderId(bidId, provider.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found"));

        if (bid.getStatus() != BidStatus.PENDING) {
            throw new BadRequestException("Only pending bids can be withdrawn");
        }

        bid.setStatus(BidStatus.WITHDRAWN);
        return BidConfirmationDto.from(bidRepository.save(bid));
    }

    // Provider view their own bids
    @Transactional(readOnly = true)
    public List<BidConfirmationDto> getMyBids(UUID userId) {
        ProviderProfile provider = providerProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));

        return bidRepository.findByProviderIdOrderByCreatedAtDesc(provider.getId())
                .stream()
                .map(BidConfirmationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isBidAccepted(UUID jobId, UUID providerId) {
        return bidRepository.existsByJobIdAndProviderIdAndStatus(
                jobId,
                providerId,
                BidStatus.ACCEPTED
        );
    }

    // 5. Helper method
    private String getActiveTierForProvider(UUID providerId) {
        return providerCreditsRepository.findById(providerId)
                .map(credits -> credits.getActiveTier() != null ? credits.getActiveTier().name() : "FREE")
                .orElse("FREE");
    }
}