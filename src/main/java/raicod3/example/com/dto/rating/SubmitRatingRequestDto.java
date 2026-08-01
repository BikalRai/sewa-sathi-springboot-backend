package raicod3.example.com.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitRatingRequestDto {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;

    @Size(max = 1000)
    private String review;
}
