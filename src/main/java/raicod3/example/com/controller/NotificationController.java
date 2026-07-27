package raicod3.example.com.controller;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.dto.email.EmailRequest;
import raicod3.example.com.model.OTPToken;
import raicod3.example.com.service.NotificationQueryService;
import raicod3.example.com.service.NotificationService;
import raicod3.example.com.service.OTPTokenService;
import raicod3.example.com.utilities.APIResponse;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/notification")
public class NotificationController {

    private final NotificationService notificationService;
    private final OTPTokenService otpTokenService;
    private final NotificationQueryService notificationQueryService;

    @PostMapping("/generate-otp")
    public ResponseEntity<APIResponse> generateOtp(@RequestBody EmailRequest req) throws MessagingException {
        log.info("Generating OTP token for email: {} ", req.getEmail());
        OTPToken otpToken = otpTokenService.generateOTPToken(req.getEmail());

        EmailRequest emailRequest = new EmailRequest(otpToken.getUser().getEmail());
        notificationService.sendEmail(emailRequest, otpToken.getOtpToken(), "/email/otp-request");

        return new ResponseEntity<>(APIResponse.success("Successfully sent email.", Http_Constants.OK), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROVIDER', 'ADMIN')")
    @GetMapping
    public APIResponse getNotifications(@AuthenticationPrincipal CustomUserDetails principal) {
        return APIResponse.success(
                notificationQueryService.getRecent(principal.getId()),
                "Fetched notifications", 200
        );
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROVIDER', 'ADMIN')")
    @GetMapping("/unread-count")
    public APIResponse getUnreadCount(@AuthenticationPrincipal CustomUserDetails principal) {
        return APIResponse.success(
                notificationQueryService.getUnreadCount(principal.getId()),
                "Fetched unread count", 200
        );
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROVIDER', 'ADMIN')")
    @PatchMapping("/{id}/read")
    public APIResponse markAsRead(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID id) {
        notificationQueryService.markAsRead(principal.getId(), id);
        return APIResponse.success(null, "Marked as read", 200);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROVIDER', 'ADMIN')")
    @PatchMapping("/read-all")
    public APIResponse markAllAsRead(@AuthenticationPrincipal CustomUserDetails principal) {
        notificationQueryService.markAllAsRead(principal.getId());
        return APIResponse.success(null, "Marked all as read", 200);
    }
}
