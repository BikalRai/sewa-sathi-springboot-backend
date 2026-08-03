package raicod3.example.com.dto.bid;

import lombok.Builder;
import raicod3.example.com.enums.BidStatus;
import raicod3.example.com.model.Bid;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record BidSummaryDto(
        UUID id,
        UUID providerId,
        String providerName,
        String providerImageUrl,
        String providerBio,
        Integer providerStartingRate,
        String providerPhone,        // conditionally populated — see note below
        String message,
        Integer quotedPrice,
        String pricingBasis,
        BidStatus status,
        Boolean contactUnlocked,
        LocalDateTime createdAt,
        String subscriptionTier
) {
    public static BidSummaryDto from(Bid bid, boolean revealPhone, String subscriptionTier) {
        return BidSummaryDto.builder()
                .id(bid.getId())
                .providerId(bid.getProvider().getId())
                .providerName(bid.getProvider().getUser().getFullName()) // adjust to your actual accessor
                .providerImageUrl(bid.getProvider().getUser().getImageUrl())
                .providerBio(bid.getProvider().getBio())
                .providerStartingRate(bid.getProvider().getStartingRate())
                .providerPhone(revealPhone ? bid.getProvider().getUser().getPhoneNumber() : null)
                .message(bid.getMessage())
                .quotedPrice(bid.getQuotedPrice())
                .pricingBasis(bid.getPricingBasis())
                .status(bid.getStatus())
                .contactUnlocked(bid.getContactUnlocked())
                .createdAt(bid.getCreatedAt())
                .subscriptionTier(subscriptionTier)
                .build();
    }
}
