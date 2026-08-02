package raicod3.example.com.dto.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import raicod3.example.com.model.JobAnalysis;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobAnalysisDto {
    private String difficulty;
    private String reasoning;
    private Double estimatedHours;
    private List<String> recommendedTools;

    // Helper constructor to easily map from the Entity
    public JobAnalysisDto(JobAnalysis analysis) {
        if (analysis != null) {
            this.difficulty = analysis.getDifficulty();
            this.reasoning = analysis.getReasoning();
            this.estimatedHours = analysis.getEstimatedHours();
            this.recommendedTools = analysis.getRecommendedTools();
        }
    }
}
