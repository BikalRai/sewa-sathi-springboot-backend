package raicod3.example.com.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.dto.esewa.EsewaInitiateRequest;
import raicod3.example.com.dto.esewa.EsewaPaymentPayloadDto;
import raicod3.example.com.enums.ErrorCode;
import raicod3.example.com.enums.PurchaseType;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.repository.CreditPurchaseRepository;
import raicod3.example.com.repository.ProviderProfileRepository;
import raicod3.example.com.service.CreditPurchaseService;
import raicod3.example.com.service.EsewaService;
import raicod3.example.com.utilities.APIResponse;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credits")
@AllArgsConstructor
public class CreditPurchaseController {
    private final CreditPurchaseService creditPurchaseService;
    private final ProviderProfileRepository providerProfileRepository;
    private final EsewaService esewaService;

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

    @PostMapping("/esewa/initiate")
    public ResponseEntity<APIResponse> initiateEsewaPayment(@RequestBody EsewaInitiateRequest request) {

        // 1. Generate a unique Order ID.
        // In a production app, you would save this 'orderId' to your database
        // with a status of 'PENDING' before sending it to eSewa.
        String orderId = UUID.randomUUID().toString();

        // 2. Define your callback URLs.
        // This is where eSewa will redirect the user after they pay.
        // You'll want to change localhost to your actual frontend domain later.
        String successUrl = "http://localhost:5173/dashboard/billing?refId=" + orderId;
        String failureUrl = "http://localhost:5173/dashboard/billing?error=payment_failed";

        // 3. Generate the payload
        EsewaPaymentPayloadDto payload = esewaService.initiatePayment(
                request.getAmount(),
                orderId,
                successUrl,
                failureUrl
        );

        // 4. Return it wrapped in your custom APIResponse
        return ResponseEntity.ok(
                APIResponse.success(
                        payload,
                        "eSewa payment payload generated successfully",
                        HttpStatus.OK.value()
                )
        );
    }

    @GetMapping("/esewa/verify")
    public ResponseEntity<APIResponse> verifyEsewaPayment(@RequestParam("refId") String refId) {

        boolean isSuccess = esewaService.verifyPayment(refId);

        if (isSuccess) {
            return ResponseEntity.ok(
                    APIResponse.success(
                            null,
                            "Payment verified successfully. Credits added to wallet.",
                            HttpStatus.OK.value()
                    )
            );
        } else {
            return ResponseEntity.badRequest().body(
                    APIResponse.error(
                            "Payment verification failed or is incomplete.",HttpStatus.BAD_REQUEST.value(), ErrorCode.ERR_BAD
                    )
            );
        }
    }
}
