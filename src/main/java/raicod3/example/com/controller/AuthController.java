package raicod3.example.com.controller;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.dto.email.EmailRequest;
import raicod3.example.com.dto.google.GoogleLoginRequestDto;
import raicod3.example.com.dto.otp.OtpRquestDto;
import raicod3.example.com.dto.user.AuthRegistrationRequestDto;
import raicod3.example.com.dto.user.AuthRequestDto;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.exception.ForbiddenException;
import raicod3.example.com.jwt.JwtUtils;
import raicod3.example.com.model.RefreshToken;
import raicod3.example.com.service.AuthService;
import raicod3.example.com.service.NotificationService;
import raicod3.example.com.service.RefreshTokenService;
import raicod3.example.com.utilities.APIResponse;
import raicod3.example.com.utilities.NumberHelper;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final NotificationService notificationService;

    public AuthController(AuthService authService, JwtUtils jwtUtils, RefreshTokenService refreshTokenService, NotificationService notificationService) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.notificationService = notificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<APIResponse> register(@RequestBody AuthRegistrationRequestDto request) throws MessagingException {
        APIResponse response = authService.registerUser(request);

        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<APIResponse> login(@RequestBody AuthRequestDto request, HttpServletResponse res) {
        log.info("Login request: {}", request.getEmail());
        APIResponse response = authService.authenticate(request, res);
        log.info("Login response: {}", response);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @GetMapping("/refresh")
    public APIResponse refreshToken(@CookieValue(name = "refreshToken") String refreshToken) {

        RefreshToken refreshTokenFromDB = refreshTokenService.getRefreshToken(refreshToken);

        String username = refreshTokenFromDB.getUser().getEmail();
        String newAccessToken = jwtUtils.generateToken(username);

        Map<String, Object> data = new HashMap<>();
        data.put("access_token", newAccessToken);

        return APIResponse.success(data, "Successfully authenticated user", Http_Constants.OK);
    }

    @PostMapping("/google")
    public ResponseEntity<APIResponse> googleLogin(@RequestBody GoogleLoginRequestDto request, HttpServletResponse response) {
        log.info("Attempting to lgin google login: {}", request);
        APIResponse res = authService.loginWithGoogle(request, response);
        log.info("Login response: {}", res);
        return new ResponseEntity<>(res,HttpStatus.OK);
    }

    @PostMapping("verify-account")
    public ResponseEntity<APIResponse> verifyAccount(@RequestBody OtpRquestDto reqDto) {
        log.info("Verify account token: {}", reqDto.getOtpToken());
        APIResponse res = authService.accountVerification(reqDto.getOtpToken());
        return new ResponseEntity<>(res,HttpStatus.OK);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<APIResponse> forgotPassword(@RequestBody EmailRequest req) throws MessagingException {
        log.info("Attempting to reset password: {}", req);

        String otpToken = NumberHelper.generateOtp();

        notificationService.sendEmail(req, otpToken, "/email/forgot-password");
        log.info("Password reset email sent");

        return new ResponseEntity<>(APIResponse.success("Email sent successfully", Http_Constants.OK), HttpStatus.OK);
    }

}