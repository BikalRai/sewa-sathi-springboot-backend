package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.annotation.Auditable;
import raicod3.example.com.dto.rating.RatingResponseDto;
import raicod3.example.com.dto.rating.SubmitRatingRequestDto;
import raicod3.example.com.enums.BidStatus;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.exception.UnauthorizedException;
import raicod3.example.com.model.*;
import raicod3.example.com.repository.CustomerRepository;
import raicod3.example.com.repository.JobRepository;
import raicod3.example.com.repository.RatingRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {
    private final RatingRepository ratingRepository;
    private final CustomerRepository customerRepository;
    private final JobRepository jobRepository;

    @Transactional
    @Auditable(action = "CUSTOMER_SUBMIT_RATING")
    public RatingResponseDto submitRating(UUID userId, UUID jobId, SubmitRatingRequestDto dto) {

        CustomerProfile customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException("Customer profile not found."));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getCustomer().getId().equals(customer.getId())) {
            throw new UnauthorizedException("This job does not belong to you.");
        }

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new BadRequestException("Job must be completed before it can be rated.");
        }

        if (ratingRepository.existsByJob_Id(jobId)) {
            throw new BadRequestException("This job has already been rated.");
        }

        if (dto.getScore() < 1 || dto.getScore() > 5) {
            throw new BadRequestException("Score must be between 1 and 5.");
        }

        ProviderProfile provider = job.getBids().stream()
                .filter(b -> b.getStatus() == BidStatus.ACCEPTED)
                .findFirst()
                .map(Bid::getProvider)
                .orElseThrow(() -> new ResourceNotFoundException("No accepted bid found for this job"));

        Rating rating = new Rating();
        rating.setJob(job);
        rating.setProvider(provider);
        rating.setCustomer(customer);
        rating.setScore(dto.getScore());
        rating.setReview(dto.getReview());

        Rating saved = ratingRepository.save(rating);

        return RatingResponseDto.from(saved);
    }
}
