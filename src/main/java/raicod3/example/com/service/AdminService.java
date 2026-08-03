package raicod3.example.com.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.dto.admin.*;
import raicod3.example.com.enums.ProviderStatus;
import raicod3.example.com.enums.PurchaseStatus;
import raicod3.example.com.enums.PurchaseType;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderService providerService;
    private final JobRepository jobRepository;
    private final BidRepository bidRepository;
    private final JobUnlockRepository jobUnlockRepository;
    private final CreditPurchaseRepository creditPurchaseRepository;


    @Transactional(readOnly = true)
    public List<PendingProviderDto> getPendingProviders() {
        List<ProviderProfile> pendingProfiles = providerProfileRepository.findByStatus(ProviderStatus.PENDING_APPROVAL);

        return pendingProfiles.stream().map(profile -> {
            String frontUrl = providerService.generateSignedKycUrl(profile.getCitizenshipFrontId());
            String backUrl = providerService.generateSignedKycUrl(profile.getCitizenshipBackId());

            return PendingProviderDto.builder()
                    .id(profile.getId())
                    .fullName(profile.getUser().getFullName())
                    .email(profile.getUser().getEmail())
                    .phoneNumber(profile.getUser().getPhoneNumber())
                    .citizenshipFrontUrl(frontUrl)
                    .citizenshipBackUrl(backUrl)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LiquidityStatDTO> getLiquidityStats() {
        // 1. Define the 7-day window (6 days ago + today)
        LocalDateTime startDate = LocalDateTime.now().minusDays(6).with(LocalTime.MIN);

        // 2. Fetch raw aggregated data from Postgres
        List<Object[]> rawJobs = jobRepository.countJobsByDate(startDate);
        List<Object[]> rawBids = bidRepository.countBidsByDate(startDate);
        List<Object[]> rawUnlocks = jobUnlockRepository.countUnlocksByDate(startDate);

        // 3. Map into fast O(1) lookups
        Map<LocalDate, Integer> jobsMap = mapRawToDateMap(rawJobs);
        Map<LocalDate, Integer> bidsMap = mapRawToDateMap(rawBids);
        Map<LocalDate, Integer> unlocksMap = mapRawToDateMap(rawUnlocks);

        // 4. Build the continuous 7-day array
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd"); // e.g., "Aug 01"
        List<LiquidityStatDTO> stats = new ArrayList<>();

        for (int i = 0; i <= 6; i++) {
            LocalDate currentDate = startDate.toLocalDate().plusDays(i);

            LiquidityStatDTO dto = new LiquidityStatDTO();
            dto.setDate(currentDate.format(formatter));
            dto.setJobs(jobsMap.getOrDefault(currentDate, 0));
            dto.setBids(bidsMap.getOrDefault(currentDate, 0));
            dto.setUnlocks(unlocksMap.getOrDefault(currentDate, 0));

            stats.add(dto);
        }

        return stats;
    }

    // Helper method to safely convert Postgres native result arrays into a Java Map
    private Map<LocalDate, Integer> mapRawToDateMap(List<Object[]> rawData) {
        Map<LocalDate, Integer> map = new HashMap<>();
        for (Object[] row : rawData) {
            if (row[0] != null && row[1] != null) {
                // Hibernate natively maps the SQL DATE() to java.time.LocalDate
                LocalDate date = (LocalDate) row[0];

                // COUNT() usually returns a BigInteger or Long in Postgres, Number handles both safely
                Number count = (Number) row[1];

                map.put(date, count.intValue());
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<AdminJobDto> getAllJobsForAdmin() {
        return jobRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public List<AdminProviderDto> getAllProvidersForAdmin() {
        List<AdminProviderDto> providers = providerProfileRepository.findAllForAdmin();

        Map<UUID, Long> bidCounts = toCountMap(bidRepository.countBidsGroupedByProvider());
        Map<UUID, Long> unlockCounts = toCountMap(jobUnlockRepository.countUnlocksGroupedByProvider());

        providers.forEach(p -> {
            p.setBidsPlaced(bidCounts.getOrDefault(p.getId(), 0L));
            p.setJobsUnlocked(unlockCounts.getOrDefault(p.getId(), 0L));
        });

        return providers;
    }

    @Transactional(readOnly = true)
    public List<AdminTransactionDto> getAllTransactionsForAdmin() {
        return creditPurchaseRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public RevenueSummaryDto getRevenueSummary() {
        List<AdminTransactionDto> all = creditPurchaseRepository.findAllForAdmin();

        long totalRevenue = 0;
        long totalTopUp = 0;
        long totalSubscription = 0;
        long completed = 0;
        long failed = 0;
        long pending = 0;

        for (AdminTransactionDto tx : all) {
            if (tx.getStatus() == PurchaseStatus.COMPLETED) {
                completed++;
                totalRevenue += tx.getAmountPaisa();
                if (tx.getPurchaseType() == PurchaseType.TOKEN_TOP_UP) {
                    totalTopUp += tx.getAmountPaisa();
                } else {
                    totalSubscription += tx.getAmountPaisa();
                }
            } else if (tx.getStatus() == PurchaseStatus.FAILED) {
                failed++;
            } else if (tx.getStatus() == PurchaseStatus.PENDING) {
                pending++;
            }
        }

        return RevenueSummaryDto.builder()
                .totalRevenuePaisa(totalRevenue)
                .totalTopUpPaisa(totalTopUp)
                .totalSubscriptionPaisa(totalSubscription)
                .completedCount(completed)
                .failedCount(failed)
                .pendingCount(pending)
                .build();
    }

    private Map<UUID, Long> toCountMap(List<Object[]> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((UUID) row[0], (Long) row[1]);
        }
        return map;
    }
}
