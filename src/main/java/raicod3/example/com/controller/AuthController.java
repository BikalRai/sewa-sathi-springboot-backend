package raicod3.example.com.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.dto.user.AuthRegistrationRequestDto;
import raicod3.example.com.dto.user.AuthRequestDto;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.exception.ForbiddenException;
import raicod3.example.com.jwt.JwtUtils;
import raicod3.example.com.model.RefreshToken;
import raicod3.example.com.service.AuthService;
import raicod3.example.com.service.RefreshTokenService;
import raicod3.example.com.utilities.APIResponse;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, JwtUtils jwtUtils, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<APIResponse> register(@RequestBody AuthRegistrationRequestDto request) {
        APIResponse response = authService.registerUser(request);

        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<APIResponse> login(@RequestBody AuthRequestDto request, HttpServletResponse res) {
        log.info("Login request: {}", request.getEmail());
        APIResponse response = authService.authenticate(request, res);
        log.info("Login response: {}", response);
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    @GetMapping("/refresh")
    public APIResponse refreshToken(@CookieValue(name = "refreshToken") String refreshToken) {

        RefreshToken refreshTokenFromDB = refreshTokenService.getRefreshToken(refreshToken);

        String username = refreshTokenFromDB.getUser().getEmail();
        String newAccessToken = jwtUtils.generateToken(username);

        Map<String, Object> data = new HashMap<>();
        data.put("access_token", newAccessToken);

        return APIResponse.success(data, "Successfully authenticated user", HttpStatus.OK);
    }
}
