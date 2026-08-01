package raicod3.example.com.dto.rating;

import raicod3.example.com.model.Rating;

import java.time.LocalDateTime;
import java.util.UUID;

public record RatingResponseDto(UUID id,
                                UUID jobId,
                                UUID providerId,
                                UUID customerId,
                                String customerName,
                                Integer score,
                                String review,
                                LocalDateTime createdAt) {

    public static RatingResponseDto from(Rating rating) {
        return new RatingResponseDto(
                rating.getId(),
                rating.getJob().getId(),
                rating.getProvider().getId(),
                rating.getCustomer().getId(),
                rating.getCustomer().getUser().getFullName(),
                rating.getScore(),
                rating.getReview(),
                rating.getCreatedAt()
        );
    }
}
