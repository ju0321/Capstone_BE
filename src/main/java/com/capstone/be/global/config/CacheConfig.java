package com.app.yeobaek_backend.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 책 페이지 내용처럼 자주 조회되지만 거의 바뀌지 않는 데이터를 Redis에 캐싱하기 위한 설정.
 * (예: CuttonPageService.getPageContent - 페이지를 넘길 때마다 매번 DB를 타지 않도록)
 * <p>
 * TTL(DEFAULT_CACHE_TTL)을 걸어서, 한동안 조회되지 않은 페이지는 자동으로 캐시에서
 * 빠지도록 한다. 특정 데이터를 캐싱하려면 서비스 메서드에 @Cacheable(cacheNames = "...")을
 * 붙이기만 하면 이 설정(TTL/직렬화 방식)이 그대로 적용된다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 캐싱 대상 DTO들은 Lombok @Jacksonized(Jackson 2 스타일 @JsonDeserialize(builder=...))로
        // 역직렬화되므로, 여기서는 신형 Jackson 3 기반 GenericJacksonJsonRedisSerializer가 아니라
        // 이 Jackson 2 기반 직렬화기를 그대로 써야 한다(Jackson 3 매퍼는 @Jacksonized가 만든
        // Jackson 2 애너테이션을 못 읽어서 기본 생성자가 없다는 에러로 캐시 HIT마다 500이 난다 -
        // 실제로 겪은 문제라 지우지 말 것).
        // .objectMapper(...)로 JavaTimeModule을 등록해야 LocalDate/LocalDateTime 필드가 있는
        // DTO(BookRecommendationResponse 등)도 캐싱할 수 있다. .defaultTyping(true)는 그 위에
        // 다형성 역직렬화용 타입 정보(@class)를 얹어준다 - 이게 없으면 캐시에서 다시 읽을 때
        // Object.class로 읽혀서 원래 타입(DTO)으로 복원되지 않는다.
        ObjectMapper redisObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer valueSerializer = GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(redisObjectMapper)
                .defaultTyping(true)
                .build();

        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_CACHE_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(configuration)
                .build();
    }
}
