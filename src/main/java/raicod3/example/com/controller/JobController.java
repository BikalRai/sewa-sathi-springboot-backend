package raicod3.example.com.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.dto.job.JobRequestDto;
import raicod3.example.com.dto.job.JobResponseDto;
import raicod3.example.com.dto.job.JobUnlockResponseDto;
import raicod3.example.com.model.Job;
import raicod3.example.com.model.JobUnlock;
import raicod3.example.com.service.JobService;
import raicod3.example.com.service.JobUnlockService;
import raicod3.example.com.utilities.APIResponse;

import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobUnlockService jobUnlockService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<APIResponse> postJob(@AuthenticationPrincipal CustomUserDetails principal, @Valid  @RequestBody JobRequestDto dto) {
        JobResponseDto result = jobService.postJob(principal.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.success(result, "Job posted successfully", Http_Constants.CREATED));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse> getAllJobs() {
        List<JobResponseDto> result = jobService.getAllJobs();

        return ResponseEntity.ok(APIResponse.success(result, "Jobs retrieved successfully", Http_Constants.OK));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<APIResponse> getMyJobs(@AuthenticationPrincipal CustomUserDetails principal) {
        List<JobResponseDto> result = jobService.getJobs(principal.getId());
        return ResponseEntity.ok(APIResponse.success(result, "Jobs retrieved successfully", Http_Constants.OK));
    }

    @PatchMapping("/{jobId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<APIResponse> cancelJob(@AuthenticationPrincipal CustomUserDetails principal,@PathVariable("jobId") UUID jobId) {
        JobResponseDto result = jobService.cancelJob(principal.getId(), jobId);

        return ResponseEntity.ok(APIResponse.success(result, "Job cancelled successfully", Http_Constants.OK));
    }

    @GetMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<APIResponse> listOpenJobs(@AuthenticationPrincipal CustomUserDetails principal) {
        List<JobResponseDto> result = jobService.getOpenJobs(principal.getId());

        return ResponseEntity.ok(APIResponse.success(result, "Jobs retrieved successfully", Http_Constants.OK));
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<APIResponse> getJobById(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("jobId") UUID jobId) {
        JobResponseDto result = jobService.getJob(jobId, principal.getId());

        return ResponseEntity.ok(APIResponse.success(result, "Job found successfully", Http_Constants.OK));
    }

    @GetMapping("/my/{jobId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<APIResponse> getMyJobById(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("jobId") UUID jobId) {
        JobResponseDto result = jobService.getMyJob(principal.getId(), jobId);
        return ResponseEntity.ok(APIResponse.success(result, "Job found successfully", Http_Constants.OK));
    }

    @PostMapping("/{jobId}/unlock")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<APIResponse> unlockJob(@PathVariable UUID jobId, @AuthenticationPrincipal CustomUserDetails principal) {
        JobUnlockResponseDto result = jobUnlockService.unlockJob(principal.getId(), jobId);

        return ResponseEntity.ok(APIResponse.success(result, "Job unlocked successfully", Http_Constants.OK));
    }

    @GetMapping("/my/unlocks")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<APIResponse> getMyUnlocks(@AuthenticationPrincipal CustomUserDetails principal) {
        List<JobUnlockResponseDto> unlocks = jobUnlockService.unlockedobs(principal.getId());

        return ResponseEntity.ok(APIResponse.success(unlocks, "List of unlocked jobs", Http_Constants.OK));
    }

    @GetMapping("/my/job-list")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<APIResponse> getMyJobsList(@AuthenticationPrincipal CustomUserDetails principal) {
        List<JobResponseDto> result = jobService.getJobsList(principal.getId());

        return ResponseEntity.ok(APIResponse.success(result, "Jobs retrieved successfully", Http_Constants.OK));
    }
}
