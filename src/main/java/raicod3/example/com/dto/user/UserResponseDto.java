package raicod3.example.com.dto.user;

import lombok.*;
import raicod3.example.com.enums.UserRole;
import raicod3.example.com.model.User;
import raicod3.example.com.model.UserAddress;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class UserResponseDto {

    private UUID id;
    private String fullName;
    private String email;
    private String role;
    private LocalDateTime createdAt;
    private boolean isActive;
    private boolean isOnboarded;
    private boolean accountLocked;
    private LocalDateTime lockedAt;
    private String address;
    private Double lat;
    private Double lng;
    private String phoneNumber;
    private String imageUrl;

    public UserResponseDto(User user ) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.phoneNumber = user.getPhoneNumber();
        this.imageUrl = user.getImageUrl();
        this.createdAt = user.getCreatedAt();
        this.isActive = user.isActive();
        this.isOnboarded = user.isOnboarded();
        this.accountLocked = user.isAccountLocked();
        this.lockedAt = user.isAccountLocked() ? user.getLockedAt() : null;
        if (user.getUserAddress() != null) {
            this.address = user.getUserAddress().getFormattedAddress();
            this.lat = user.getUserAddress().getLatitude();
            this.lng = user.getUserAddress().getLongitude();
        } else {
            this.address = null;
            this.lat = null;
            this.lng = null;
        }
    }
}
