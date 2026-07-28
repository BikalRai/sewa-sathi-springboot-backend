package raicod3.example.com.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KycRejectionDto {
    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
