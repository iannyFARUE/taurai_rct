package com.hexicode.taurai_rtc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.hexicode.taurai_rtc.security.JwtService;
import com.hexicode.taurai_rtc.services.UserService;
import com.hexicode.taurai_rtc.utils.CallSignalingHandler;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    @Value("application.firewalls.cors.allowed-origins")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new CallSignalingHandler(jwtService,userService), "/call-signaling")
                .setAllowedOrigins(allowedOrigins.split(",")); // Configure properly for production
    }
}
