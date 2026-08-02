package raicod3.example.com.dto.provider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.enums.ProviderStatus;
import raicod3.example.com.enums.SubscriptionTier;
import raicod3.example.com.model.ProviderCredits;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.model.User;
import raicod3.example.com.model.UserAddress;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProviderResponseDto {

    private UUID id;
    private String gender;
    private ProviderStatus status;
    private String rejectionReason;
    private List<String> workDistrict;
    private List<String> services;
    private String bio;
    private String pricingBasis;
    private Integer startingRate;
    private UserResponseDto user;

    // --- FINANCIAL & SUBSCRIPTION FIELDS ---
    private SubscriptionTier activeTier;
    private Integer tokenBalance;
    private LocalDateTime subscriptionExpiresAt;

    private Double latitude;
    private Double longitude;
    private String address;

    private String experience;

    public ProviderResponseDto(ProviderProfile profile, User user, ProviderCredits credits) {
        this.id = profile.getId();
        this.status = profile.getStatus();
        this.rejectionReason = profile.getRejectionReason();
        this.gender = profile.getGender();
        this.bio = profile.getBio();
        this.pricingBasis = profile.getPricingBasis();
        this.startingRate = profile.getStartingRate();
        this.user = new UserResponseDto(user);

        this.workDistrict = profile.getWorkDistrict() != null
                ? new ArrayList<>(profile.getWorkDistrict())
                : new ArrayList<>();

        this.services = profile.getServices() != null
                ? new ArrayList<>(profile.getServices())
                : new ArrayList<>();

        if (credits != null) {
            this.activeTier = credits.getActiveTier() != null ? credits.getActiveTier() : SubscriptionTier.FREE;
            this.tokenBalance = credits.getBalance();
            this.subscriptionExpiresAt = credits.getSubscriptionExpiresAt();
        } else {
            this.activeTier = SubscriptionTier.FREE;
            this.tokenBalance = 0;
            this.subscriptionExpiresAt = null;
        }

        UserAddress userAddress = user.getUserAddress();
        if (userAddress != null) {
            this.latitude = userAddress.getLatitude();
            this.longitude = userAddress.getLongitude();
            this.address = userAddress.getFormattedAddress();
        }

        this.experience = profile.getExperience();
    }
}