package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;
import raicod3.example.com.config.EsewaConfig;
import raicod3.example.com.dto.esewa.EsewaPaymentPayloadDto;
import raicod3.example.com.dto.esewa.EsewaStatusResponseDto;
import raicod3.example.com.lib.esewa.EsewaCryptoUtils;

@Service
@RequiredArgsConstructor
public class EsewaService {
    private final EsewaConfig esewaConfig;
    private final RestTemplate restTemplate;

    /**
     * Generates the payload required by the frontend to initiate an eSewa payment.
     *
     * @param amount The base cost of the tokens/subscription in Rupees.
     * @param orderId Your database's internal tracking ID for this pending purchase.
     * @param successUrl Where eSewa should redirect upon successful payment.
     * @param failureUrl Where eSewa should redirect upon failure.
     * @return EsewaPaymentPayloadDto
     */
    public EsewaPaymentPayloadDto initiatePayment(double amount, String orderId, String successUrl, String failureUrl) {

        // eSewa strictly expects string values, often without trailing decimals if they are zero,
        // but standard string representation of a double works.
        String amountStr = String.valueOf(amount);
        String taxAmount = "0"; // Adjust if you charge tax
        String serviceCharge = "0";
        String deliveryCharge = "0";
        String totalAmount = String.valueOf(amount); // amount + tax + service + delivery

        // eSewa requires knowing exactly which fields were used to generate the signature
        String signedFieldNames = "total_amount,transaction_uuid,product_code";

        // Generate the secure signature
        String signature = EsewaCryptoUtils.generateSignature(
                totalAmount,
                orderId,
                esewaConfig.getMerchantCode(),
                esewaConfig.getSecretKey()
        );

        // Build and return the payload to the frontend
        return EsewaPaymentPayloadDto.builder()
                .amount(amountStr)
                .tax_amount(taxAmount)
                .total_amount(totalAmount)
                .transaction_uuid(orderId)
                .product_code(esewaConfig.getMerchantCode())
                .product_service_charge(serviceCharge)
                .product_delivery_charge(deliveryCharge)
                .success_url(successUrl)
                .failure_url(failureUrl)
                .signed_field_names(signedFieldNames)
                .signature(signature)
                .gateway_url(esewaConfig.getGatewayUrl())
                .build();
    }

    /**
     * Verifies the transaction directly with eSewa's servers.
     *
     * @param transactionUuid The order ID you generated during initiation
     * @return boolean True if payment is complete and valid
     */
    public boolean verifyPayment(String transactionUuid) {

        // 1. [DATABASE LOOKUP]
        // You MUST look up the transaction in your database using the transactionUuid.
        // Order order = orderRepository.findByOrderId(transactionUuid).orElseThrow();
        // double expectedAmount = order.getAmount();

        // For this example, let's assume the expected amount we pulled from the DB is 100.0
        double expectedAmount = 100.0;
        String amountStr = String.valueOf(expectedAmount);

        // 2. Build the verification URL
        String url = esewaConfig.getStatusUrl() +
                "?product_code=" + esewaConfig.getMerchantCode() +
                "&total_amount=" + amountStr +
                "&transaction_uuid=" + transactionUuid;

        try {
            // 3. Make the Server-to-Server call
            ResponseEntity<EsewaStatusResponseDto> response = restTemplate.getForEntity(url, EsewaStatusResponseDto.class);
            EsewaStatusResponseDto statusResponse = response.getBody();

            // 4. Validate the response
            if (statusResponse != null && "COMPLETE".equalsIgnoreCase(statusResponse.getStatus())) {

                // 5. [DATABASE UPDATE]
                // The payment is real!
                // Mark the order as PAID in your database.
                // Add the credits to the provider's wallet.
                // order.setStatus("PAID");
                // orderRepository.save(order);

                return true;
            }

            return false;

        } catch (Exception e) {
            // Log the error (e.g., eSewa is down, timeout, etc.)
            System.err.println("Failed to verify eSewa payment: " + e.getMessage());
            return false;
        }
    }
}
