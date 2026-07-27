package raicod3.example.com.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.service.AdminService;
import raicod3.example.com.utilities.APIResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/providers/pending")
    public ResponseEntity<APIResponse> getPendingProviders() {
        var providers = adminService.getPendingProviders();
        return ResponseEntity.ok(
                APIResponse.success(providers, "Fetched pending providers", HttpStatus.OK.value())
        );
    }

    @PatchMapping("/providers/{providerId}/verify")
    public ResponseEntity<APIResponse> verifyProvider(@PathVariable UUID providerId) {
        var verifiedProvider = adminService.verifyProvider(providerId);
        return ResponseEntity.ok(
                APIResponse.success(verifiedProvider, "Provider successfully verified", HttpStatus.OK.value())
        );
    }
}
