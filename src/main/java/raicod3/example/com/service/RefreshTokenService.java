package raicod3.example.com.service;

import org.springframework.stereotype.Service;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.RefreshToken;
import raicod3.example.com.repository.RefreshTokenRepository;

import java.time.LocalDateTime;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public RefreshToken getRefreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findRefreshTokenByRefreshToken(refreshToken).orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        if(token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new BadRequestException("Refresh token expired");
        }

        return token;
    }
}
