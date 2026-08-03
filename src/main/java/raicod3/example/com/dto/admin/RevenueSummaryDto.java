package raicod3.example.com.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RevenueSummaryDto {
    private long totalRevenuePaisa;
    private long totalTopUpPaisa;
    private long totalSubscriptionPaisa;
    private long completedCount;
    private long failedCount;
    private long pendingCount;
}
