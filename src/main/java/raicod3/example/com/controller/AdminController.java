package raicod3.example.com.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.dto.admin.*;
import raicod3.example.com.service.AdminService;
import raicod3.example.com.service.ProviderService;
import raicod3.example.com.utilities.APIResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;
    private final ProviderService providerService;

    @GetMapping("/providers/pending")
    public ResponseEntity<APIResponse> getPendingProviders() {
        var providers = adminService.getPendingProviders();
        return ResponseEntity.ok(
                APIResponse.success(providers, "Fetched pending providers", HttpStatus.OK.value())
        );
    }

    @GetMapping("liquidity-stats")
    public ResponseEntity<APIResponse> getLiquidityStats() {
        List<LiquidityStatDTO> liquidityStats = adminService.getLiquidityStats();

        return ResponseEntity.ok(APIResponse.success(liquidityStats, "Fetched liquidity stats", HttpStatus.OK.value()));
    }

    @GetMapping("/jobs")
    public ResponseEntity<APIResponse> getAllJobs() {
        List<AdminJobDto> jobs = adminService.getAllJobsForAdmin();
        return ResponseEntity.ok(
                APIResponse.success(jobs, "Fetched all jobs", HttpStatus.OK.value())
        );
    }

    @PostMapping("providers/{providerId}/approve")
    public APIResponse approveKyc(@PathVariable UUID providerId) {
        return providerService.approveProviderKyc(providerId);
    }

    @PostMapping("providers/{providerId}/reject")
    public APIResponse rejectKyc(
            @PathVariable UUID providerId,
            @Valid @RequestBody KycRejectionDto request) {
        return providerService.rejectProviderKyc(providerId, request);
    }

    @GetMapping("/providers")
    public ResponseEntity<APIResponse> getAllProviders() {
        List<AdminProviderDto> providers = adminService.getAllProvidersForAdmin();
        return ResponseEntity.ok(
                APIResponse.success(providers, "Fetched all providers", HttpStatus.OK.value())
        );
    }

    @GetMapping("/transactions")
    public ResponseEntity<APIResponse> getAllTransactions() {
        List<AdminTransactionDto> transactions = adminService.getAllTransactionsForAdmin();
        return ResponseEntity.ok(
                APIResponse.success(transactions, "Fetched all transactions", HttpStatus.OK.value())
        );
    }

    @GetMapping("/transactions/summary")
    public ResponseEntity<APIResponse> getRevenueSummary() {
        RevenueSummaryDto summary = adminService.getRevenueSummary();
        return ResponseEntity.ok(
                APIResponse.success(summary, "Fetched revenue summary", HttpStatus.OK.value())
        );
    }
}
