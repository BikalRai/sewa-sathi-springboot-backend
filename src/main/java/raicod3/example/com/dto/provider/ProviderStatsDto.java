package raicod3.example.com.dto.provider;

public record ProviderStatsDto(int totalEarned,
                               int thisMonthEarned,
                               long activeJobs,
                               Double avgRating,
                               long ratingCount) {
}
