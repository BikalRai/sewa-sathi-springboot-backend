package raicod3.example.com.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.dto.provider.*;
import raicod3.example.com.service.ProviderService;
import raicod3.example.com.utilities.APIResponse;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;

    @PreAuthorize("hasRole('PROVIDER')")
    @GetMapping("/me/credits")
    public ResponseEntity<APIResponse> getProviderCredits(@AuthenticationPrincipal CustomUserDetails principal) {
        ProviderCreditsResponseDto credits = providerService.getProviderCredits(principal.getId());

        return ResponseEntity.ok(APIResponse.success(credits, "Provider credits fetched successfully.", HttpStatus.OK.value()));
    }

    // patch personal details
    @PreAuthorize("hasRole('PROVIDER')")
    @PatchMapping("/update-personal")
    public ResponseEntity<APIResponse> updatePersonal(@RequestBody OnboardingProviderRequestDto request, Principal principal) {
        APIResponse res = providerService.updateProviderProfile(request, principal.getName());

        return ResponseEntity.ok(res);
    }

    @PreAuthorize("hasRole('PROVIDER')")
    @PatchMapping("/me")
    public ResponseEntity<APIResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ProviderProfileUpdateDto dto
    ) {
        ProviderResponseDto updated = providerService.updateProfile(principal.getId(), dto);
        return ResponseEntity.ok(
                APIResponse.success(updated, "Profile updated successfully", HttpStatus.OK.value())
        );
    }

    @PreAuthorize("hasRole('PROVIDER')")
    @PostMapping("/kyc")
    public APIResponse submitKyc(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody KycSubmissionRequestDto request) {

        // Pass the securely extracted user ID down to the service layer
        return providerService.submitKycDocuments(principal.getUser().getId(), request);
    }

    @PreAuthorize("hasRole('PROVIDER')")
    @GetMapping("/me")
    public ResponseEntity<APIResponse> getProviderProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        ProviderResponseDto result = providerService.findByUserId(principal.getUser().getId());

        return ResponseEntity.ok(APIResponse.success(result, "Provider profile fetched successfully.", HttpStatus.OK.value()));
    }

    @GetMapping("/{providerId}/public")
    public ResponseEntity<APIResponse> getPublicProfile(@PathVariable UUID providerId) {
        PublicProviderProfileDto result = providerService.getPublicProfile(providerId);

        return ResponseEntity.ok(APIResponse.success(result, "Provider profile fetched successfully.", HttpStatus.OK.value()));
    }
}
