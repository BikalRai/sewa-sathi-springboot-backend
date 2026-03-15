package raicod3.example.com.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PasswordUpdateRequestDto {
    private String email;
    private String password;
    private String confirmPassword;
}
