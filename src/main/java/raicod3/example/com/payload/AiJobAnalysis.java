package raicod3.example.com.payload;

import java.util.List;

public record AiJobAnalysis(
        String difficulty,
        String reasoning,
        Double estimatedHours,
        List<String> recommendedTools
) {
}
