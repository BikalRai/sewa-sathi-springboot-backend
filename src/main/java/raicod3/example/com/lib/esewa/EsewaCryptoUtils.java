package raicod3.example.com.lib.esewa;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EsewaCryptoUtils {
    /**
     * Generates the HMAC SHA256 Base64 encoded signature required by eSewa v2.
     *
     * @param totalAmount     The exact amount being charged (e.g., "100" or "100.0")
     * @param transactionUuid Your system's unique order/transaction ID
     * @param merchantCode    The eSewa merchant code (e.g., "EPAYTEST")
     * @param secretKey       The eSewa secret key
     * @return Base64 encoded signature string
     */
    public static String generateSignature(String totalAmount, String transactionUuid, String merchantCode, String secretKey) {
        try {
            // 1. Construct the message string exactly as eSewa demands
            String message = totalAmount + "," + transactionUuid + "," + merchantCode;

            // 2. Initialize the HMAC SHA256 MAC instance
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            // 3. Compute the hash
            byte[] hashBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            // 4. Base64 encode the output
            return Base64.getEncoder().encodeToString(hashBytes);

        } catch (Exception e) {
            // In a production app, you might want to throw a custom PaymentGatewayException here
            throw new RuntimeException("Failed to generate eSewa cryptographic signature", e);
        }
    }
}
