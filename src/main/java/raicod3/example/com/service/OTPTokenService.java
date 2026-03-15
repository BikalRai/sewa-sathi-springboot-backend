package raicod3.example.com.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import raicod3.example.com.dto.otp.OtpRquestDto;
import raicod3.example.com.enums.TokenType;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.OTPToken;
import raicod3.example.com.model.User;
import raicod3.example.com.repository.OTPTokenRepository;
import raicod3.example.com.repository.UserRepository;
import raicod3.example.com.utilities.NumberHelper;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class OTPTokenService {

    private final OTPTokenRepository otpTokenRepository;
    private final UserRepository userRepository;

    public OTPTokenService(OTPTokenRepository otpTokenRepository, UserRepository userRepository) {
        this.otpTokenRepository = otpTokenRepository;
        this.userRepository = userRepository;
    }

    public OTPToken generateOTPToken(String email) {

        log.info("Email in service: {} ", email);

        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        otpTokenRepository.findByUser(user).ifPresent(otpTokenRepository::delete);

        String token = NumberHelper.generateOtp();

        OTPToken otpToken = new OTPToken(token, user, TokenType.GENERATION);
        otpToken.setOtpExpires(LocalDateTime.now().plusMinutes(5));
        otpTokenRepository.save(otpToken);

        return otpToken;
    }

    public OTPToken getOTPTokenByOtpToken(String otpToken) {
        return otpTokenRepository.findOTPTokenByOtpToken(otpToken).orElseThrow(() -> new BadRequestException("Invalid OTP Token"));

    }

    public Boolean validateOTPToken(OtpRquestDto otpToken) {
        OTPToken existingToken = otpTokenRepository.findOTPTokenByOtpToken(otpToken.getOtpToken()).orElseThrow(() -> new BadRequestException("Invalid OTP Token"));

        if(existingToken.getOtpExpires().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP Token expired");
        }

        return true;
    }

    public Boolean deleteOTPTokenById(UUID otpTokenId) {
        OTPToken existingToken = otpTokenRepository.findById(otpTokenId).orElseThrow(() -> new ResourceNotFoundException("Token not found"));

        otpTokenRepository.deleteById(existingToken.getId());
        return true;
    }

    public void deleteOTPToken(OTPToken otpToken) {
        OTPToken existingToken = otpTokenRepository.findOTPTokenByOtpToken(otpToken.getOtpToken()).orElseThrow(() -> new BadRequestException("Invalid OTP Token"));

        otpTokenRepository.delete(existingToken);

    }
}
