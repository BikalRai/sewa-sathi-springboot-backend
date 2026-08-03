package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raicod3.example.com.model.JobUnlock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobUnlockRepository extends JpaRepository<JobUnlock, UUID> {

    boolean existsByJob_IdAndProvider_Id(UUID jobId, UUID providerId);

    List<JobUnlock> findAllByProviderId(UUID providerId);

    List<JobUnlock> findByProviderIdAndJobIdIn(UUID providerId, List<UUID> jobIds);

    int countByJob_Id(UUID jobId);

    @Query(value = "SELECT DATE(created_at) as eventDate, COUNT(id) as totalCount " +
            "FROM job_unlocks " +
            "WHERE created_at >= :startDate " +
            "GROUP BY DATE(created_at)", nativeQuery = true)
    List<Object[]> countUnlocksByDate(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT u.provider.id, COUNT(u) FROM JobUnlock u GROUP BY u.provider.id")
    List<Object[]> countUnlocksGroupedByProvider();
}
