package raicod3.example.com.dto.provider;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProviderProfileUpdateDto {

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotEmpty(message = "At least one service must be provided")
    private List<String> services;

    @NotBlank(message = "Experience level is required")
    private String experience;

    @NotEmpty(message = "At least one work area must be provided")
    private List<String> workArea;

    @NotBlank(message = "Bio is required")
    private String bio;

    @NotBlank(message = "Pricing basis is required")
    private String pricingBasis;

    @NotNull(message = "Starting rate cannot be null")
    @Min(value = 0, message = "Starting rate cannot be negative")
    private Integer startingRate;

    @NotNull(message = "Latitude is required")
    @Min(value = -90, message = "Latitude must be valid (between -90 and 90)")
    @Max(value = 90, message = "Latitude must be valid (between -90 and 90)")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @Min(value = -180, message = "Longitude must be valid (between -180 and 180)")
    @Max(value = 180, message = "Longitude must be valid (between -180 and 180)")
    private Double longitude;

    @NotBlank(message = "Address is required")
    private String address;
}
