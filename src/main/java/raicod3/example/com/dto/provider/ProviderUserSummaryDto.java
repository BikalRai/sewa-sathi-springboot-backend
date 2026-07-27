package raicod3.example.com.dto.provider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProviderUserSummaryDto {
    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String imageUrl;
    private LocalDateTime createdAt;

    public ProviderUserSummaryDto(raicod3.example.com.model.User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.imageUrl = user.getImageUrl();
        this.createdAt = user.getCreatedAt();
    }
}