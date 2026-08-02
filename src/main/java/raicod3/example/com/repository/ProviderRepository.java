package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raicod3.example.com.model.ProviderProfile;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<ProviderProfile, UUID> {

    ProviderProfile findByUserId(UUID userId);

    @Query(value = """
        SELECT p.* FROM providers p
        INNER JOIN users u ON p.user_id = u.id
        INNER JOIN user_addresses a ON u.id = a.user_id
        INNER JOIN provider_services ps ON p.id = ps.profile_id
        WHERE p.is_active = true 
        AND p.is_verified = true
        AND ps.service = :categoryName
        AND (
            6371 * acos(
                cos(radians(:jobLat)) * cos(radians(a.latitude)) * 
                cos(radians(a.longitude) - radians(:jobLon)) + 
                sin(radians(:jobLat)) * sin(radians(a.latitude))
            )
        ) <= :radiusKm
    """, nativeQuery = true)
    List<ProviderProfile> findProvidersWithinRadius(
            @Param("jobLat") Double jobLat,
            @Param("jobLon") Double jobLon,
            @Param("categoryName") String categoryName,
            @Param("radiusKm") Double radiusKm
    );

}
