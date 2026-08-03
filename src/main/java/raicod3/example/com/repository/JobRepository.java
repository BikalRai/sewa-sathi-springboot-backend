package raicod3.example.com.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raicod3.example.com.dto.admin.AdminJobDto;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.model.Job;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);

    @Query("SELECT j FROM Job j " +
            "JOIN FETCH j.category " +
            "JOIN FETCH j.customer c " +
            "JOIN FETCH c.user " +
            "LEFT JOIN FETCH j.bids")
    List<Job> findAllWithDetails();

    // Notice the query is now checking j.category.name
    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.category.name IN :providerServices ORDER BY j.createdAt DESC")
    List<Job> findByStatusAndCategoryNameInOrderByCreatedAtDesc(
            @Param("status") JobStatus status,
            @Param("providerServices") List<String> providerServices
    );

    List<Job> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    List<Job> findByStatusAndExpiresAtBefore(JobStatus status, LocalDateTime now);

    @Query("SELECT COUNT(b) FROM Bid b WHERE b.job.id = :jobId AND b.status != 'WITHDRAWN'")
    int countActiveBids(@Param("jobId") UUID jobId);

    @Query("SELECT COUNT(b) FROM Bid b WHERE b.job.id = :jobId AND b.provider.id = :providerId")
    int countBidByProvider(@Param("jobId") UUID jobId, @Param("providerId") UUID providerId);

    // JobRepository
    @Query("SELECT j FROM Job j JOIN FETCH j.category WHERE j.id = :id")
    Optional<Job> findByIdWithCategory(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Job j WHERE j.id = :id")
    Optional<Job> findByIdWithWriteLock(@Param("id") UUID id);

    @Query(value = "SELECT DATE(created_at) as eventDate, COUNT(id) as totalCount " +
            "FROM jobs " +
            "WHERE created_at >= :startDate " +
            "GROUP BY DATE(created_at)", nativeQuery = true)
    List<Object[]> countJobsByDate(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT new raicod3.example.com.dto.admin.AdminJobDto(" +
            "j.id, j.description, j.status, c.user.fullName, " +
            "COUNT(DISTINCT b.id), j.unlockCount, " +
            "j.createdAt, j.expiresAt) " +
            "FROM Job j " +
            "JOIN j.customer c " +
            "LEFT JOIN Bid b ON b.job = j " +
            "GROUP BY j.id, j.description, j.status, c.user.fullName, j.unlockCount, j.createdAt, j.expiresAt " +
            "ORDER BY j.createdAt DESC")
    List<AdminJobDto> findAllForAdmin();
}
