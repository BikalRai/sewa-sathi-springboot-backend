package raicod3.example.com.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raicod3.example.com.enums.SubscriptionTier;


import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "provider_credits")
public class ProviderCredits {

    @Id
    private UUID providerId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "provider_id")
    private ProviderProfile provider;

    @Column(nullable = false)
    private Integer balance = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier activeTier = SubscriptionTier.FREE;

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Domain Logic: Checks if the subscription is still valid based on the clock.
     * This ensures that even if the cron job fails to downgrade them,
     * the system inherently knows their tier has expired.
     */
    public boolean isSubscriptionActive() {
        return this.activeTier != SubscriptionTier.FREE
                && this.subscriptionExpiresAt != null
                && this.subscriptionExpiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Always use this method to check their tier, never getActiveTier() directly.
     * It forces an automatic fallback to FREE if the clock has run out.
     */
    public SubscriptionTier getEffectiveTier() {
        return isSubscriptionActive() ? this.activeTier : SubscriptionTier.FREE;
    }
}
