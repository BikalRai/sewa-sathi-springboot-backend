package raicod3.example.com.service;

import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import raicod3.example.com.custom.CustomUserDetailsService;
import raicod3.example.com.dto.google.GoogleLoginRequestDto;
import raicod3.example.com.dto.user.AuthRegistrationRequestDto;
import raicod3.example.com.dto.user.AuthRequestDto;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.enums.AuthProvider;
import raicod3.example.com.enums.UserRole;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ForbiddenException;
import raicod3.example.com.jwt.JwtUtils;
import raicod3.example.com.model.RefreshToken;
import raicod3.example.com.model.User;
import raicod3.example.com.repository.RefreshTokenRepository;
import raicod3.example.com.repository.UserRepository;
import raicod3.example.com.utilities.APIResponse;
import raicod3.example.com.utilities.PasswordValidation;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;


    public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager, CustomUserDetailsService customUserDetailsService, JwtUtils jwtUtils, PasswordEncoder passwordEncoder, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public APIResponse registerUser(AuthRegistrationRequestDto request) {


        Optional<User> foundUser = userRepository.findUserByEmail(request.getEmail());

        if (foundUser.isPresent()) {
            throw new BadRequestException("Email already registered.");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        if (request.getRole() == null) {
            throw new BadRequestException("Role not supported.");
        }

        if (request.getRole() != null && request.getRole().equalsIgnoreCase("provider")) {
            user.setRole(UserRole.PROVIDER);
        } else {
            user.setRole(UserRole.CUSTOMER);
        }

        user.setCreatedAt(LocalDateTime.now());

        String passwordValidation = PasswordValidation.validatePassword(request.getPassword());

        if (!"Strong".equalsIgnoreCase(passwordValidation)) {
            throw new BadRequestException(passwordValidation);
        }

        User savedUser = userRepository.save(user);

        UserResponseDto userResponseDto = new UserResponseDto(savedUser);

        return APIResponse.success(userResponseDto, "Successfully registered user", HttpStatus.CREATED);
    }

    public APIResponse authenticate(AuthRequestDto request, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage());
            throw new UsernameNotFoundException("Invalid credentials.");
        }
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getEmail());


        User user = userRepository.findUserByEmail(request.getEmail()).orElseThrow(() -> new ForbiddenException("Account not found."));
        user.setProvider(AuthProvider.LOCAL);


        String accessToken = jwtUtils.generateToken(userDetails.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(userDetails.getUsername());
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);

        refreshTokenRepository.save(new RefreshToken(refreshToken, expiry, user));

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
//                .secure(true)
                .path("/api/v1/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
//                .sameSite("Strict")
                .build();

        response.setHeader("Set-Cookie", cookie.toString());

        Map<String, Object> data = new HashMap<>();
        data.put("access_token", accessToken);
        data.put("role", user.getRole());
        data.put("userId", user.getId());

        return APIResponse.success(data, "Successfully authenticated user", HttpStatus.OK);
    }

    public APIResponse loginWithGoogle(GoogleLoginRequestDto request, HttpServletResponse response) {
        String idToken = request.getIdToken();

        String[] parts = idToken.split("\\.");
        if (parts.length != 2) {
            throw new BadRequestException("Invalid ID Token.");
        }

        String payloadJson = new String(Base64.getDecoder().decode(parts[1]));
        Map<String, Object> payload = new Gson().fromJson(payloadJson, Map.class);

        String sub = (String) payload.get("sub");
        String email = (String) payload.get("email");
        String fullName = (String) payload.get("name");

        Optional<User> existing = userRepository.findByProviderId(sub);

        User user;

        if (existing.isPresent()) {
            user = existing.get();
        } else {
            Optional<User> foundUser = userRepository.findUserByEmail(email);
            if (foundUser.isPresent()) {
                user = foundUser.get();
                user.setProvider(AuthProvider.GOOGLE);
                user.setProviderId(sub);
                user.setActive(true);
                userRepository.save(user);
            } else {
                user = new User();
                user.setEmail(email);
                user.setFullName(fullName);
                user.setProvider(AuthProvider.GOOGLE);
                user.setProviderId(sub);
                user.setActive(true);
                user.setRole(UserRole.CUSTOMER);
                user.setCreatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        }

        String accessToken = jwtUtils.generateToken(user.getEmail());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);

        refreshTokenRepository.save(new RefreshToken(refreshToken, expiry, user));

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
//                .secure(true)
                .path("/api/v1/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
//                .sameSite("Strict")
                .build();

        response.setHeader("Set-Cookie", cookie.toString());

        Map<String, Object> data = new HashMap<>();
        data.put("access_token", accessToken);
        data.put("role", user.getRole());
        data.put("userId", user.getId());

        return APIResponse.success(data, "Successfully authenticated user", HttpStatus.OK);
    }

}
