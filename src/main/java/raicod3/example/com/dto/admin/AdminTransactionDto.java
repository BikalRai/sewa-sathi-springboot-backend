package raicod3.example.com.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import raicod3.example.com.enums.PurchaseStatus;
import raicod3.example.com.enums.PurchaseType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminTransactionDto {
    private UUID id;
    private String pidx;
    private String providerName;
    private PurchaseType purchaseType;
    private Integer creditsRequested;
    private Integer amountPaisa;
    private PurchaseStatus status;
    private LocalDateTime createdAt;
}
