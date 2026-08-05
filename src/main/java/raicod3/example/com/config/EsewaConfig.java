package raicod3.example.com.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "esewa")
@Getter
@Setter
public class EsewaConfig {
    private String merchantCode;
    private String secretKey;
    private String gatewayUrl;
    private String statusUrl;
}
