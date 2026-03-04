package raicod3.example.com.dto.user;

import lombok.*;
import raicod3.example.com.enums.UserRole;
import raicod3.example.com.model.User;

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
    private boolean accountLocked;
    private LocalDateTime lockedAt;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.createdAt = user.getCreatedAt();
        this.isActive = user.isActive();
        this.accountLocked = user.isAccountLocked();
        this.lockedAt = user.isAccountLocked() ? user.getLockedAt() : null;
    }
}
