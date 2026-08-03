package raicod3.example.com.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LiquidityStatDTO {
    private String date;
    private int jobs;
    private int bids;
    private int unlocks;
}
