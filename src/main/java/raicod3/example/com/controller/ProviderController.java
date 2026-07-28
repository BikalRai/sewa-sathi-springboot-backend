package raicod3.example.com.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.dto.provider.KycSubmissionRequestDto;
import raicod3.example.com.dto.provider.OnboardingProviderRequestDto;
import raicod3.example.com.dto.provider.ProviderCreditsResponseDto;
import raicod3.example.com.dto.provider.ProviderResponseDto;
import raicod3.example.com.service.ProviderService;
import raicod3.example.com.utilities.APIResponse;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')") // locks out customer
public class ProviderController {

    private final ProviderService providerService;

    @GetMapping("/me/credits")
    public ResponseEntity<APIResponse> getProviderCredits(@AuthenticationPrincipal CustomUserDetails principal) {
        ProviderCreditsResponseDto credits = providerService.getProviderCredits(principal.getId());

        return ResponseEntity.ok(APIResponse.success(credits, "Provider credits fetched successfully.", HttpStatus.OK.value()));
    }

    // patch personal details
    @PatchMapping("/update-personal")
    public ResponseEntity<APIResponse> updatePersonal(@RequestBody OnboardingProviderRequestDto request, Principal principal) {
        APIResponse res = providerService.updateProviderProfile(request, principal.getName());

        return ResponseEntity.ok(res);
    }

    @PostMapping("/kyc")
    public APIResponse submitKyc(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody KycSubmissionRequestDto request) {

        // Pass the securely extracted user ID down to the service layer
        return providerService.submitKycDocuments(principal.getUser().getId(), request);
    }

    @GetMapping("/me")
    public ResponseEntity<APIResponse> getProviderProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        ProviderResponseDto result = providerService.findByUserId(principal.getUser().getId());

        return ResponseEntity.ok(APIResponse.success(result, "Provider profile fetched successfully.", HttpStatus.OK.value()));
    }
}
