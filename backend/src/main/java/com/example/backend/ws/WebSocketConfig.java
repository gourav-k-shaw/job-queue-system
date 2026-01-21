package com.example.backend.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Clients subscribe to these destinations
        registry.enableSimpleBroker("/topic");

        // Client -> server messages will start with /app
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint for frontend to connect
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // for dev, later restrict origin
    }
}
