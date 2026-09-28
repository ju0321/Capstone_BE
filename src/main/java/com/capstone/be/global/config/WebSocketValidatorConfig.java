package com.app.yeobaek_backend.global.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

// WebSocketConfig와 순환 참조 방지를 위해 Validator 등록을 분리
@Configuration
@RequiredArgsConstructor
public class WebSocketValidatorConfig {

    private final SimpAnnotationMethodMessageHandler simpAnnotationMethodMessageHandler;

    @PostConstruct
    public void configureValidator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet(); // ValidatorFactory 초기화 (미호출 시 NPE)
        simpAnnotationMethodMessageHandler.setValidator(validator); // @Valid 동작을 위한 Validator 등록
    }
}
