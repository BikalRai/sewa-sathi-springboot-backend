package raicod3.example.com.enums;

import lombok.Getter;

@Getter
public enum SubscriptionTier {
    FREE(0, 45),       // 0 included, Rs 45 per token
    PRO(15, 35),       // 15 included, Rs 35 per token
    BUSINESS(50, 25);  // 50 included, Rs 25 per token

    private final int includedTokens;
    private final int tokenPriceRs;

    SubscriptionTier(int includedTokens, int tokenPriceRs) {
        this.includedTokens = includedTokens;
        this.tokenPriceRs = tokenPriceRs;
    }

}
