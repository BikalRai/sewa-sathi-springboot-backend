package raicod3.example.com.dto.job;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CompleteJobRequestDto {

    @Size(max = 500, message = "Completion notes cannot exceed 500 characters")
    private String completionNotes;

    @Size(max = 5, message = "Maximum of 5 proof images allowed")
    private List<String> completionImages;
}
