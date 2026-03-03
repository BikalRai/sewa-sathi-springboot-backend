package raicod3.example.com.dto.user;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class AuthRequestDto {
    private String email;
    private String password;
}
