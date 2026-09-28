package com.app.yeobaek_backend.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ObjectMapper를 스프링 빈으로 직접 등록한다.
 * spring-boot-starter-json을 추가해도 자동으로 ObjectMapper 빈이 안 잡히는 문제가 있어서,
 * BookService처럼 ObjectMapper를 직접 주입받아 쓰는 코드가 동작하도록 여기서 명시적으로 빈을 만든다.
 * JavaTimeModule을 등록해서 LocalDate/LocalDateTime 같은 자바 8 시간 타입도 문제없이 직렬화/역직렬화되게 한다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
