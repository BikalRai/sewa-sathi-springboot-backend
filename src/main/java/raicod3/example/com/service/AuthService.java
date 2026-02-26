package raicod3.example.com.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import raicod3.example.com.custom.CustomUserDetailsService;
import raicod3.example.com.dto.user.AuthRegistrationRequestDto;
import raicod3.example.com.dto.user.AuthRequestDto;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.jwt.JwtUtils;
import raicod3.example.com.model.User;
import raicod3.example.com.repository.UserRepository;
import raicod3.example.com.utilities.APIResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager, CustomUserDetailsService customUserDetailsService, JwtUtils jwtUtils, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
    }

    public APIResponse registerUser(AuthRegistrationRequestDto request) {
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        UserResponseDto userResponseDto = new UserResponseDto(savedUser);

        return APIResponse.success(userResponseDto, "Successfully registered user", HttpStatus.CREATED);
    }

    public APIResponse authenticate(AuthRequestDto request) {

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getEmail());


        String accessToken = jwtUtils.generateToken(userDetails.getUsername());

        Map<String, String> data = new HashMap<>();
        data.put("access_token", accessToken);

        return APIResponse.success(data, "Successfully authenticated user", HttpStatus.OK);
    }
}
