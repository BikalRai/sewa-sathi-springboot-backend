package raicod3.example.com.dto.esewa;

import lombok.Data;

@Data
public class EsewaStatusResponseDto {
    private String product_code;
    private String transaction_uuid;
    private String total_amount;
    private String status; // We are looking for "COMPLETE"
    private String ref_id;
}
