package raicod3.example.com.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raicod3.example.com.enums.PurchaseStatus;
import raicod3.example.com.enums.PurchaseType;
import raicod3.example.com.enums.SubscriptionTier;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.lib.khalti.KhaltiClient;
import raicod3.example.com.model.CreditPurchase;
import raicod3.example.com.model.ProviderCredits;
import raicod3.example.com.model.ProviderProfile;
import raicod3.example.com.repository.CreditPurchaseRepository;
import raicod3.example.com.repository.ProviderCreditsRepository;
import raicod3.example.com.repository.ProviderProfileRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class CreditPurchaseService {

    private final KhaltiClient khaltiClient;
    private final CreditPurchaseRepository creditPurchaseRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderCreditsRepository providerCreditsRepository;

    @Transactional
    public Map<String, Object> initiatePurchase(UUID providerId, int creditsRequested, PurchaseType purchaseType) {
        ProviderProfile providerProfile = providerProfileRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider Profile Not Found"));

        ProviderCredits providerCredits = providerCreditsRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider Credits Not Found"));

        int amountPaisa = 0;
        int finalCreditsToGrant = 0;
        String purchaseName = "";

        // 1. Calculate pricing securely on the server
        switch (purchaseType) {
            case TOKEN_TOP_UP:
                if (creditsRequested <= 0) throw new BadRequestException("Must request at least 1 credit");

                // Determine price based on their CURRENT active tier
                SubscriptionTier activeTier = providerCredits.getEffectiveTier();
                int pricePerCreditPaisa = switch (activeTier) {
                    case PRO -> 3500;      // Rs 35
                    case BUSINESS -> 2500; // Rs 25
                    default -> 4500;       // Rs 45 (FREE)
                };

                amountPaisa = creditsRequested * pricePerCreditPaisa;
                finalCreditsToGrant = creditsRequested;
                purchaseName = creditsRequested + " Sewalo Lead Credits";
                break;

            case SUBSCRIPTION_PRO:
                amountPaisa = 50000; // Rs 500
                finalCreditsToGrant = 15;
                purchaseName = "Sewalo Pro Partner (30 Days)";
                break;

            case SUBSCRIPTION_BUSINESS:
                amountPaisa = 150000; // Rs 1500
                finalCreditsToGrant = 50;
                purchaseName = "Sewalo Verified Business (30 Days)";
                break;

            default:
                throw new BadRequestException("Invalid purchase type");
        }

        // 2. Build Khalti Payload
        Map<String, Object> payload = Map.of(
                "return_url", "http://localhost:5173/payment/verify",
                "website_url", "http://localhost:5173",
                "amount", amountPaisa,
                "purchase_order_id", "SEWALO-" + UUID.randomUUID(),
                "purchase_order_name", purchaseName,
                "customer_info", Map.of(
                        "name", providerProfile.getUser().getFullName(),
                        "email", providerProfile.getUser().getEmail(),
                        "phone", providerProfile.getUser().getPhoneNumber() != null ? providerProfile.getUser().getPhoneNumber() : "9800000000"
                )
        );

        Map<String, Object> khaltiResponse = khaltiClient.initiatePayment(payload);

        String pidx = (String) khaltiResponse.get("pidx");
        String paymentUrl = (String) khaltiResponse.get("payment_url");

        // 3. Save to Ledger
        CreditPurchase purchase = new CreditPurchase();
        purchase.setProvider(providerProfile);
        purchase.setPidx(pidx);
        purchase.setPurchaseType(purchaseType);
        purchase.setCreditsRequested(finalCreditsToGrant); // Save the calculated tokens, not the raw input
        purchase.setAmountPaisa(amountPaisa);
        purchase.setStatus(PurchaseStatus.PENDING);
        creditPurchaseRepository.save(purchase);

        return Map.of("payment_url", paymentUrl, "pidx", pidx);
    }

    @Transactional
    public String verifyPurchase(String pidx) {
        CreditPurchase purchase = creditPurchaseRepository.findByPidx(pidx)
                .orElseThrow(() -> new ResourceNotFoundException("Credit Purchase record Not Found"));

        if (purchase.getStatus() == PurchaseStatus.COMPLETED) {
            return "Already processed";
        }

        Map<String, Object> lookupResult = khaltiClient.lookupPayment(pidx);
        String status = (String) lookupResult.get("status");

        if ("Completed".equals(status)) {
            // 1. Mark ledger as Complete
            purchase.setStatus(PurchaseStatus.COMPLETED);
            creditPurchaseRepository.save(purchase);

            // 2. Update Provider's Wallet & Subscription Status
            ProviderCredits credits = providerCreditsRepository.findById(purchase.getProvider().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Provider Credits Not Found"));

            // Always add the tokens
            credits.setBalance(credits.getBalance() + purchase.getCreditsRequested());

            // If it was a subscription, upgrade the tier and add 30 days
            if (purchase.getPurchaseType() == PurchaseType.SUBSCRIPTION_PRO) {
                credits.setActiveTier(SubscriptionTier.PRO);
                credits.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(30));
            } else if (purchase.getPurchaseType() == PurchaseType.SUBSCRIPTION_BUSINESS) {
                credits.setActiveTier(SubscriptionTier.BUSINESS);
                credits.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(30));
            }

            // Save the updated state
            providerCreditsRepository.save(credits);

            return "Payment verified. Wallet and subscription updated successfully.";
        } else {
            purchase.setStatus(PurchaseStatus.FAILED);
            creditPurchaseRepository.save(purchase);
            throw new BadRequestException("Payment not completed. Status: " + status);
        }
    }
}