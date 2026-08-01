package raicod3.example.com.dto.provider;

import raicod3.example.com.dto.rating.RatingResponseDto;

import java.util.List;
import java.util.UUID;

public record PublicProviderProfileDto(UUID providerId,
                                       String fullName,
                                       String imageUrl,
                                       List<String> services,
                                       Double avgRating,
                                       long ratingCount,
                                       List<RatingResponseDto> recentReviews) {
}
