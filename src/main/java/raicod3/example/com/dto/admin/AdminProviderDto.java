package raicod3.example.com.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import raicod3.example.com.enums.ProviderStatus;
import raicod3.example.com.enums.SubscriptionTier;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminProviderDto {
    private UUID id;
    private String fullName;
    private String email;
    private ProviderStatus status;
    private Integer creditBalance;   // null-safe: 0 if no ProviderCredits row
    private SubscriptionTier tier;   // null-safe: FREE if no ProviderCredits row
    private long jobsUnlocked;
    private long bidsPlaced;
    private LocalDateTime joinedAt;
}
