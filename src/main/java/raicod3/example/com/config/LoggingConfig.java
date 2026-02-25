package raicod3.example.com.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class LoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true); // shows JSON body from POSTMAN
        loggingFilter.setMaxPayloadLength(10000); // Limits how much body text is logged
        loggingFilter.setIncludeHeaders(false); // Keeps logs clean by removing headers
        loggingFilter.setAfterMessagePrefix("Request Data : ");

        return loggingFilter;
        }
}
