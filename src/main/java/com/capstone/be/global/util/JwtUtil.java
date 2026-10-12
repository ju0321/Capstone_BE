package com.capstone.be.global.util;

import com.capstone.be.global.exception.CustomException;
import com.capstone.be.global.exception.GlobalErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final Key key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final StringRedisTemplate redisTemplate;

    private static final String ACCESS_TOKEN_BLACKLIST_PREFIX = "blacklist:access:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    // TODO(TEST ONLY): 1~4 는 실제 JWT 서명/만료 검증 없이 해당 userId로 곧바로 인증되는 테스트용 우회 토큰입니다.
    //  운영 배포 전 반드시 제거해야 합니다.
    private static final Set<String> TEST_BYPASS_TOKENS = Set.of("1", "2", "3", "4");

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            StringRedisTemplate redisTemplate) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.redisTemplate = redisTemplate;
    }

    // 테스트 우회 유저(userId 1~4) 여부를 반환합니다. 이 유저들은 토큰 무효화 대상에서 제외됩니다.
    public boolean isTestBypassUser(Long userId) {
        return TEST_BYPASS_TOKENS.contains(String.valueOf(userId));
    }

    // userId를 subject로 담아 access token을 생성합니다.
    // TODO(TEST ONLY): 테스트 유저(userId 1~4)는 실제 JWT 대신 userId 문자열 자체를 토큰으로 반환합니다.
    public String generateAccessToken(Long userId) {
        if (TEST_BYPASS_TOKENS.contains(String.valueOf(userId))) {
            return String.valueOf(userId);
        }
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(key)
                .compact();
    }

    // userId를 subject로 담아 refresh token을 생성하고, 이후 로그아웃 시 무효화할 수 있도록 Redis에 저장합니다.
    // TODO(TEST ONLY): 테스트 유저(userId 1~4)는 실제 JWT 대신 userId 문자열 자체를 토큰으로 반환하며, Redis에 저장하지 않습니다.
    public String generateRefreshToken(Long userId) {
        if (TEST_BYPASS_TOKENS.contains(String.valueOf(userId))) {
            return String.valueOf(userId);
        }
        String refreshToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(key)
                .compact();
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId, refreshToken, Duration.ofMillis(refreshTokenExpiration));
        return refreshToken;
    }

    // 토큰을 검증하고 subject(userId)를 추출합니다. 위조/만료/로그아웃된(블랙리스트) 토큰이면 INVALID_TOKEN 예외를 던집니다.
    // TODO(TEST ONLY): 토큰 값이 1~4 이면 실제 서명/만료/블랙리스트 검증 없이 해당 userId로 곧바로 인증 처리합니다.
    public Long getUserIdFromToken(String token) {
        if (TEST_BYPASS_TOKENS.contains(token)) {
            return Long.parseLong(token);
        }
        Claims claims = parseClaims(token);
        if (!TOKEN_TYPE_ACCESS.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(ACCESS_TOKEN_BLACKLIST_PREFIX + token))) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }
        return Long.parseLong(claims.getSubject());
    }

    // 로그아웃 시 access token을 남은 만료시간만큼 블랙리스트에 등록하고, 저장된 refresh token은 즉시 삭제합니다.
    // 테스트 우회 유저(1~4)는 계속 재사용할 수 있도록 무효화 대상에서 제외합니다.
    public void invalidateTokens(Long userId, String accessToken) {
        if (isTestBypassUser(userId)) {
            return;
        }
        Claims claims = parseClaims(accessToken);
        long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remaining > 0) {
            redisTemplate.opsForValue().set(
                    ACCESS_TOKEN_BLACKLIST_PREFIX + accessToken, "logout", Duration.ofMillis(remaining));
        }
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(key.getEncoded()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }
    }
}
