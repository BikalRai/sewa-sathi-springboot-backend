package raicod3.example.com.dto.user;


import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class AuthRegistrationRequestDto {

    private String fullName;
    private String email;
    private String password;
    private String role;

}
