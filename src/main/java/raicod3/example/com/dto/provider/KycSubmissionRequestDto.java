package raicod3.example.com.dto.provider;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KycSubmissionRequestDto {
    @NotBlank(message = "Citizenship front image is required")
    private String citizenshipFrontId;

    @NotBlank(message = "Citizenship back image is required")
    private String citizenshipBackId;
}
