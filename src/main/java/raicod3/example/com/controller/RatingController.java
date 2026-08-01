package raicod3.example.com.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.dto.rating.RatingResponseDto;
import raicod3.example.com.dto.rating.SubmitRatingRequestDto;
import raicod3.example.com.service.RatingService;
import raicod3.example.com.utilities.APIResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;

    @PostMapping("/{jobId}/rate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<APIResponse> submitRating(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("jobId") UUID jobId,
            @Valid @RequestBody SubmitRatingRequestDto dto) {

        RatingResponseDto result = ratingService.submitRating(principal.getId(), jobId, dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.success(result, "Rating submitted successfully", Http_Constants.CREATED));
    }
}
