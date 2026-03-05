package raicod3.example.com.controller;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.dto.email.EmailRequest;
import raicod3.example.com.model.OTPToken;
import raicod3.example.com.service.NotificationService;
import raicod3.example.com.service.OTPTokenService;
import raicod3.example.com.utilities.APIResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/notification")
public class NotificationController {

    private final NotificationService notificationService;
    private final OTPTokenService otpTokenService;

    public NotificationController(NotificationService notificationService, OTPTokenService otpTokenService) {
        this.notificationService = notificationService;
        this.otpTokenService = otpTokenService;
    }

    @PostMapping("/generate-otp")
    public ResponseEntity<APIResponse> generateOtp(@RequestBody EmailRequest req) throws MessagingException {
        log.info("Generating OTP token for email: {} ", req.getEmail());
        OTPToken otpToken = otpTokenService.generateOTPToken(req.getEmail());


        EmailRequest emailRequest = new EmailRequest(otpToken.getUser().getEmail(), "Request for OTP");
        notificationService.sendEmail(emailRequest, otpToken.getOtpToken(), "/email/otp-request");

        return new ResponseEntity<>(APIResponse.success("Successfully sent email.", Http_Constants.OK), HttpStatus.OK);
    }


}
