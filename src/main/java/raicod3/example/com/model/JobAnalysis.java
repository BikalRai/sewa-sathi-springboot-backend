package raicod3.example.com.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobAnalysis {

    @Id
    private UUID jobId; // Same ID as the Job

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Tells Hibernate to use the Job's ID as this entity's ID
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "difficulty") // Assuming you have a String or Enum here
    private String difficulty;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    private Double estimatedHours;

    // This is the PostgreSQL magic. No join tables needed.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> recommendedTools;
}
