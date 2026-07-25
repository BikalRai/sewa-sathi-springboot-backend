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

}
