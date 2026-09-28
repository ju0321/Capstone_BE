package com.app.yeobaek_backend.global.handler;

import com.app.yeobaek_backend.domain.discussion_room.enums.DiscussionRoomMemberStatus;
import com.app.yeobaek_backend.domain.discussion_room.repository.DiscussionRoomMemberRepository;
import com.app.yeobaek_backend.global.exception.CustomException;
import com.app.yeobaek_backend.global.exception.GlobalErrorCode;
import com.app.yeobaek_backend.global.util.JwtUtil;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * STOMP CONNECT 시점에 Authorization 헤더의 JWT를 검증하고, 이후 메시지에서 꺼내 쓸 수 있도록
 * userId를 세션 attribute에 저장한다. (일반 REST API에서 매 요청마다 헤더로 인증하는 것과 동일한 방식)
 * 세션에 Principal도 붙여서 SimpMessagingTemplate.convertAndSendToUser()로 개인 알림을 보낼 수 있게 한다.
 * SUBSCRIBE 시에는 /sub/rooms/{roomId} 구독자가 해당 방의 승인된 멤버인지 검사한다.
 */
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    public static final String SESSION_USER_ID = "userId";

    private static final Pattern ROOM_DESTINATION_PATTERN = Pattern.compile("^/sub/rooms/(\\d+)$");

    private final JwtUtil jwtUtil;
    private final DiscussionRoomMemberRepository discussionRoomMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT == accessor.getCommand()) {
            Long userId = resolveUserId(accessor.getFirstNativeHeader("Authorization"));
            accessor.getSessionAttributes().put(SESSION_USER_ID, userId);

            String principalName = String.valueOf(userId);
            accessor.setUser((Principal) () -> principalName);

            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (StompCommand.SUBSCRIBE == accessor.getCommand()) {
            assertRoomSubscriptionAllowed(accessor);
        }

        return message;
    }

    private void assertRoomSubscriptionAllowed(StompHeaderAccessor accessor) {
        Long roomId = extractRoomId(accessor.getDestination());
        if (roomId == null) {
            return;
        }

        Long userId = (Long) accessor.getSessionAttributes().get(SESSION_USER_ID);
        boolean isApprovedMember = discussionRoomMemberRepository.findByUserIdAndRoomId(userId, roomId)
                .map(member -> member.getStatus() == DiscussionRoomMemberStatus.APPROVED)
                .orElse(false);

        if (!isApprovedMember) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }
    }

    private Long extractRoomId(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = ROOM_DESTINATION_PATTERN.matcher(destination);
        return matcher.matches() ? Long.parseLong(matcher.group(1)) : null;
    }

    private Long resolveUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }
        return jwtUtil.getUserIdFromToken(authorizationHeader.substring("Bearer ".length()));
    }
}
