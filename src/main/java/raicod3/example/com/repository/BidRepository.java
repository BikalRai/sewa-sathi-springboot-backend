package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raicod3.example.com.enums.BidStatus;
import raicod3.example.com.model.Bid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {

    boolean existsByJobIdAndProviderIdAndStatus(UUID jobId, UUID providerId, BidStatus status);

    // All bids on a job — homeowner uses this
    List<Bid> findByJobIdOrderByCreatedAtAsc(UUID jobId);

    // All bids by a provider — provider dashboard
    List<Bid> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    // Check if provider already bid on this job
    boolean existsByJobIdAndProviderId(UUID jobId, UUID providerId);

    // Count active (non-withdrawn) bids on a job — enforce max 3
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.job.id = :jobId AND b.status != 'WITHDRAWN'")
    int countActiveBids(@Param("jobId") UUID jobId);

    // For accepting a bid — need to reject all others
    List<Bid> findByJobIdAndStatusNot(UUID jobId, BidStatus status);

    Optional<Bid> findByIdAndProviderId(UUID bidId, UUID providerId);

    Optional<Bid> findByJobIdAndProviderId(UUID jobId, UUID providerId);

    List<Bid> findByProviderIdAndJobIdIn(UUID providerId, List<UUID> jobIds);

    @Query("SELECT b FROM Bid b JOIN FETCH b.job j WHERE b.provider.id = :providerId AND b.status IN :statuses ORDER BY b.createdAt DESC")
    List<Bid> findByProviderIdAndStatusInWithJobs(
            @Param("providerId") UUID providerId,
            @Param("statuses") List<BidStatus> statuses
    );

    @Query(value = "SELECT DATE(created_at) as eventDate, COUNT(id) as totalCount " +
            "FROM bids " +
            "WHERE created_at >= :startDate " +
            "GROUP BY DATE(created_at)", nativeQuery = true)
    List<Object[]> countBidsByDate(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT b.provider.id, COUNT(b) FROM Bid b GROUP BY b.provider.id")
    List<Object[]> countBidsGroupedByProvider();
}
