package raicod3.example.com.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.enums.PurchaseType;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.repository.CreditPurchaseRepository;
import raicod3.example.com.repository.ProviderProfileRepository;
import raicod3.example.com.service.CreditPurchaseService;
import raicod3.example.com.utilities.APIResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/credits")
@AllArgsConstructor
public class CreditPurchaseController {
    private final CreditPurchaseService creditPurchaseService;
    private final ProviderProfileRepository providerProfileRepository;

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<APIResponse> initiate(
            @RequestParam(required = false, defaultValue = "0") Integer creditsRequested,
            @RequestParam PurchaseType purchaseType,
            @AuthenticationPrincipal CustomUserDetails principal) {

        var provider = providerProfileRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));

        Map<String, Object> result = creditPurchaseService.initiatePurchase(provider.getId(), creditsRequested, purchaseType);

        return ResponseEntity.ok(APIResponse.success(result, "Payment initiated", HttpStatus.OK.value()));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<APIResponse> verify(@RequestParam String pidx) {
        String result = creditPurchaseService.verifyPurchase(pidx);
        return ResponseEntity.ok(APIResponse.success(result, "Payment verified", HttpStatus.OK.value()));
    }
}
