package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raicod3.example.com.model.Rating;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    boolean existsByJob_Id(UUID jobId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.provider.id = :providerId")
    Double findAverageScoreByProviderId(@Param("providerId") UUID providerId);

    long countByProvider_Id(UUID providerId);

    Optional<Rating> findByJob_Id(UUID jobId);

    List<Rating> findByJob_IdIn(List<UUID> jobIds);

    List<Rating> findTop10ByProvider_IdOrderByCreatedAtDesc(UUID providerId);
}
