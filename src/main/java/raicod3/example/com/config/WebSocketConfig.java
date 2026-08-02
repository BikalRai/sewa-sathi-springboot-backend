package raicod3.example.com.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import raicod3.example.com.jwt.JwtUtils;
import raicod3.example.com.custom.CustomUserDetailsService; // <-- Adjust to your actual service
import raicod3.example.com.custom.CustomUserDetails; // <-- Adjust to your actual UserDetails

import java.security.Principal;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService; // <-- Inject this to get the UUID

    public WebSocketConfig(JwtUtils jwtUtils, CustomUserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        throw new IllegalArgumentException("Missing or invalid Authorization header");
                    }

                    String token = authHeader.substring(7);

                    if (!jwtUtils.validateToken(token)) {
                        throw new IllegalArgumentException("Invalid JWT token");
                    }

                    // 1. Get the email from the token
                    String username = jwtUtils.getUsername(token);

                    // 2. Fetch the full user profile from the DB to get their UUID
                    // (Note: If your JWT already contains the UUID as a claim, skip the DB call
                    // and just do `String userId = jwtUtils.getUserId(token);`)
                    CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);
                    String userId = userDetails.getId().toString();

                    // 3. Create a custom Principal that explicitly returns the UUID
                    Principal userPrincipal = new Principal() {
                        @Override
                        public String getName() {
                            return userId; // <-- This is the magic line. STOMP will now route by UUID.
                        }
                    };

                    accessor.setUser(userPrincipal);
                }

                return message;
            }
        });
    }
}