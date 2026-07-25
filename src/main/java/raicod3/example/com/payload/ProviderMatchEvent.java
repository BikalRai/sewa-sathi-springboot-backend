package raicod3.example.com.payload;

import java.util.UUID;

public record ProviderMatchEvent(
        UUID jobId,
        String category,
        String description
){}
