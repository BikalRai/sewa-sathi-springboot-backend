package raicod3.example.com.dto.provider;

import lombok.*;
import raicod3.example.com.model.ProviderProfile;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class ProviderAdminResponseDto {
    private UUID id;
    private String gender;
    private String experience;
    private List<String> services;
    private List<String> workDistrict;
    private String bio;
    private String pricingBasis;
    private Integer startingRate;
    private Boolean isVerified;
    private Boolean isActive;

    private ProviderUserSummaryDto user;

    public ProviderAdminResponseDto(ProviderProfile provider) {
        this.id = provider.getId();
        this.gender = provider.getGender();
        this.experience = provider.getExperience();
        this.services = provider.getServices() != null ? List.copyOf(provider.getServices()) : List.of();
        this.workDistrict = provider.getWorkDistrict() != null ? List.copyOf(provider.getWorkDistrict()) : List.of();
        this.bio = provider.getBio();
        this.pricingBasis = provider.getPricingBasis();
        this.startingRate = provider.getStartingRate();
        this.isVerified = provider.getIsVerified();
        this.isActive = provider.getIsActive();
        this.user = provider.getUser() != null ? new ProviderUserSummaryDto(provider.getUser()) : null;
    }

}
