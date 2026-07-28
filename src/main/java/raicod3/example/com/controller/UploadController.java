package raicod3.example.com.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.utilities.APIResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final Cloudinary cloudinary;

    // for public assets
    @GetMapping("/cloudinary-signature")
    public APIResponse getUploadSignature(@AuthenticationPrincipal CustomUserDetails principal) {
        long timestamp = System.currentTimeMillis() / 1000;

        // Only these params are authorized — frontend cannot deviate from them
        Map<String, Object> paramsToSign = ObjectUtils.asMap(
                "timestamp", timestamp,
                "folder", "sewalo/jobs"
        );

        String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);

        Map<String, Object> responseData = ObjectUtils.asMap(
                "signature", signature,
                "timestamp", timestamp,
                "apiKey", cloudinary.config.apiKey,
                "cloudName", cloudinary.config.cloudName,
                "folder", "sewalo/jobs"
        );

        return APIResponse.success(responseData, "Signature generated", 200);
    }

    // secure endpoint for KYC documents
    @GetMapping("/cloudinary-kyc-signature")
    public APIResponse getUploadKycSignature(@AuthenticationPrincipal CustomUserDetails principal) {
        long timestamp = System.currentTimeMillis() / 1000;

        // We explicitly inject "type": "authenticated".
        // This is the mathematical lock that forces Cloudinary to make the file private.
        Map<String, Object> paramsToSign = ObjectUtils.asMap(
                "timestamp", timestamp,
                "folder", "sewalo/kyc_docs",
                "type", "authenticated"
        );

        String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);

        // We must return "type" to the frontend because React has to append it
        // to the FormData object when uploading.
        Map<String, Object> responseData = ObjectUtils.asMap(
                "signature", signature,
                "timestamp", timestamp,
                "apiKey", cloudinary.config.apiKey,
                "cloudName", cloudinary.config.cloudName,
                "folder", "sewalo/kyc_docs",
                "type", "authenticated"
        );

        return APIResponse.success(responseData, "Secure KYC Signature generated", HttpStatus.OK.value());
    }
}
