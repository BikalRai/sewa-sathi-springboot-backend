package raicod3.example.com.dto.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCustomerProfileRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;
    private String phoneNumber;
    private Double latitude;
    private Double longitude;
    private String address;
    private String imageUrl;
}
