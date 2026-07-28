package raicod3.example.com.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PendingProviderDto {
    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String citizenshipFrontUrl; // This will hold the TEMPORARY SIGNED URL
    private String citizenshipBackUrl;  // This will hold the TEMPORARY SIGNED URL
}
