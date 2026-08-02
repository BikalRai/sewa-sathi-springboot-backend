package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raicod3.example.com.model.JobUnlock;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobUnlockRepository extends JpaRepository<JobUnlock, UUID> {

    boolean existsByJob_IdAndProvider_Id(UUID jobId, UUID providerId);

    List<JobUnlock> findAllByProviderId(UUID providerId);

    List<JobUnlock> findByProviderIdAndJobIdIn(UUID providerId, List<UUID> jobIds);

    int countByJob_Id(UUID jobId);
}
