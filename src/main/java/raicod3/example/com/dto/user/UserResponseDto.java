package raicod3.example.com.dto.user;

import lombok.*;
import raicod3.example.com.model.User;

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

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
    }
}
