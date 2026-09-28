package com.app.yeobaek_backend.global.handler;

import com.app.yeobaek_backend.global.util.JwtUtil;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * WebSocket 핸드셰이크(HTTP 업그레이드) 시점에 세션의 Principal을 결정한다.
 * SimpMessagingTemplate.convertAndSendToUser()가 유저-세션을 매칭하려면 이 시점에 Principal이 있어야 한다
 * (STOMP CONNECT 헤더는 이후 시점이라 너무 늦음). 접속 URL의 token 쿼리파라미터에서 JWT를 꺼내 검증한다.
 * 토큰이 없거나 유효하지 않으면 익명(null)으로 두고, 실제 인증 거부는 StompHandler가 CONNECT 프레임에서 처리한다.
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtUtil jwtUtil;

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");

        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            String name = String.valueOf(userId);
            return () -> name;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
