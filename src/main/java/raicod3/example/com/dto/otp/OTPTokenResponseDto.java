package raicod3.example.com.dto.otp;

import lombok.*;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class OTPTokenResponseDto {
    private UUID id;

    private String otpToken;

    private LocalDateTime otpExpires;

    private UserResponseDto user;

    public OTPTokenResponseDto(UUID id, String otpToken, LocalDateTime otpExpires, User user) {
        this.id = id;
        this.otpToken = otpToken;
        this.otpExpires = otpExpires;
        this.user = new UserResponseDto(user);
    }
}
