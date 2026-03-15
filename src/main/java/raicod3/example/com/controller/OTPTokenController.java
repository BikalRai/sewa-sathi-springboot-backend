package raicod3.example.com.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.dto.otp.OtpRquestDto;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.model.OTPToken;
import raicod3.example.com.service.OTPTokenService;
import raicod3.example.com.utilities.APIResponse;

@RestController
@RequestMapping("/api/v1/otp")
public class OTPTokenController {

    private final OTPTokenService otpTokenService;

    public OTPTokenController(OTPTokenService otpTokenService) {
        this.otpTokenService = otpTokenService;
    }

    @PostMapping("validate-token")
    public ResponseEntity<APIResponse> validateToken(@RequestBody OtpRquestDto otpToken) {
        Boolean valid = otpTokenService.validateOTPToken(otpToken);
        if(!valid) {
            throw new BadRequestException("Invalid OTP Token");
        }

       OTPToken token = otpTokenService.getOTPTokenByOtpToken(otpToken.getOtpToken());

        otpTokenService.deleteOTPToken(token);

        return ResponseEntity.ok(APIResponse.success("OTP token validated successfully.", Http_Constants.OK));
    }
}
