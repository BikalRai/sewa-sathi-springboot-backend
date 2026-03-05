package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raicod3.example.com.model.OTPToken;
import raicod3.example.com.model.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OTPTokenRepository extends JpaRepository<OTPToken, UUID> {
    Optional<OTPToken> findOTPTokenByOtpToken(String otpToken);

    Long deleteOTPTokenByOtpToken(OTPToken existingToken);

    Optional<OTPToken> findByUser(User user);
}
