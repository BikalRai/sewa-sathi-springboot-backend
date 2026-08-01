package raicod3.example.com.dto.provider;

import raicod3.example.com.enums.SubscriptionTier;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProviderCreditsResponseDto(UUID providerId, Integer balance, SubscriptionTier activeTier, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime subscriptionExpiresAt) {
}
