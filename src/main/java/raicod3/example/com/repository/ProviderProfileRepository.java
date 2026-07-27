package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raicod3.example.com.model.ProviderProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, UUID> {
    Optional<ProviderProfile> findByUserId(UUID userId);


    @Query("SELECT p FROM ProviderProfile p JOIN FETCH p.user JOIN p.services s WHERE LOWER(s) = :category AND p.isActive = true")
    List<ProviderProfile> findByServiceIgnoreCase(@Param("category") String category);

    List<ProviderProfile> findByIsVerifiedFalse();

    @Query("SELECT p FROM ProviderProfile p LEFT JOIN FETCH p.services LEFT JOIN FETCH p.user WHERE p.id = :id")
    Optional<ProviderProfile> findByIdWithServices(@Param("id") UUID id);

    @Query("SELECT DISTINCT p FROM ProviderProfile p LEFT JOIN FETCH p.services LEFT JOIN FETCH p.user WHERE p.isVerified = false")
    List<ProviderProfile> findByIsVerifiedFalseWithServices();


}
