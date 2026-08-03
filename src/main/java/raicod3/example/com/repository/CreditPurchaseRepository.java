package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import raicod3.example.com.dto.admin.AdminTransactionDto;
import raicod3.example.com.model.CreditPurchase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditPurchaseRepository extends JpaRepository<CreditPurchase, UUID> {

    Optional<CreditPurchase> findByPidx(String pidx);

    @Query("SELECT new raicod3.example.com.dto.admin.AdminTransactionDto(" +
            "cp.id, cp.pidx, cp.provider.user.fullName, cp.purchaseType, " +
            "cp.creditsRequested, cp.amountPaisa, cp.status, cp.createdAt) " +
            "FROM CreditPurchase cp " +
            "ORDER BY cp.createdAt DESC")
    List<AdminTransactionDto> findAllForAdmin();
}
