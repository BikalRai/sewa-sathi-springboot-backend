package raicod3.example.com.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class OTPToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String otpToken;

    private LocalDateTime otpExpires;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public OTPToken(String otpToken, User user) {
        this.otpToken = otpToken;
        this.otpExpires = LocalDateTime.now().plusMinutes(5);
        this.user = user;
    }

}
