package raicod3.example.com.dto.provider;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.enums.ProviderStatus;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.model.User;

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

    public ProviderResponseDto(ProviderProfile profile, User user) {
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
    }
}
