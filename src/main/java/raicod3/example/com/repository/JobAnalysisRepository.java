package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raicod3.example.com.model.JobAnalysis;

import java.util.UUID;

@Repository
public interface JobAnalysisRepository extends JpaRepository<JobAnalysis, UUID> {
}
