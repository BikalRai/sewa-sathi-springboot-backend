package raicod3.example.com.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import raicod3.example.com.enums.JobStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminJobDto {
    private UUID id;
    private String description;
    private JobStatus status;
    private String customerName;
    private long bidCount;
    private Integer unlockCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}