package raicod3.example.com.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import raicod3.example.com.dto.user.AuthRegistrationRequestDto;
import raicod3.example.com.dto.user.AuthRequestDto;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.service.AuthService;
import raicod3.example.com.utilities.APIResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<APIResponse> register(@RequestBody AuthRegistrationRequestDto request) {
        APIResponse response = authService.registerUser(request);

        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<APIResponse> login(@RequestBody AuthRequestDto request) {
        log.info("Login request: {}", request.getEmail());
        APIResponse response = authService.authenticate(request);
        log.info("Login response: {}", response);
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }
}
