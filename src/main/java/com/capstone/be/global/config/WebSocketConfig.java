package com.app.yeobaek_backend.global.config;

import com.app.yeobaek_backend.global.handler.JwtHandshakeHandler;
import com.app.yeobaek_backend.global.handler.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final long HEARTBEAT_INTERVAL_MS = 10000;

    private final StompHandler stompHandler;
    private final JwtHandshakeHandler jwtHandshakeHandler;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * 서버<->클라이언트 STOMP heartbeat 전송을 전담할 스케줄러. enableSimpleBroker()의
     * setHeartbeatValue()가 이 스케줄러로 10초마다 하트비트 프레임을 보내고 받는지 확인한다 -
     * TCP는 안 끊겼는데 네트워크가 죽어서 응답이 없는 "좀비 커넥션"을 감지하기 위함이다.
     */
    @Bean
    public TaskScheduler heartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub") // 클라이언트가 메시지를 받을 때 구독하는 경로 (Broker 역할)
                .setHeartbeatValue(new long[] {HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS})
                .setTaskScheduler(heartbeatTaskScheduler());
        registry.setApplicationDestinationPrefixes("/pub"); // 클라이언트가 서버로 메시지를 보낼 때 붙일 prefix (Controller 역할)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // 클라이언트가 웹소켓 연결을 맺기 위해 접속하는 엔드포인트
                .setAllowedOriginPatterns(allowedOrigins) // CORS 허용
                .setHandshakeHandler(jwtHandshakeHandler)
                .withSockJS(); // SockJS 지원 (웹소켓 미지원 브라우저 대응)
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler); // 인터셉터 등록
    }
}
