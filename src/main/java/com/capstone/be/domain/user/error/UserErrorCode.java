package com.capstone.be.domain.user.error;

import com.capstone.be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    PASSWORD_CONFIRM_MISMATCH("U001", "비밀번호와 비밀번호 확인이 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    NICKNAME_DUPLICATED("U002", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    EMAIL_DUPLICATED("U003", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    LOGIN_FAILED("U004", "아이디 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}
