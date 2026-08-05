package raicod3.example.com.dto.esewa;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EsewaPaymentPayloadDto {
    private String amount;
    private String tax_amount;
    private String total_amount;
    private String transaction_uuid;
    private String product_code;
    private String product_service_charge;
    private String product_delivery_charge;
    private String success_url;
    private String failure_url;
    private String signed_field_names;
    private String signature;

    // We also pass the URL so the frontend knows where to submit the form
    private String gateway_url;
}
