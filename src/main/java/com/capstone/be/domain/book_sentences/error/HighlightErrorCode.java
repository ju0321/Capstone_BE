package com.capstone.be.domain.book_sentences.error;

import com.capstone.be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum HighlightErrorCode implements BaseErrorCode {

    HIGHLIGHT_NOT_FOUND("HL001", "존재하지 않는 하이라이트입니다.", HttpStatus.NOT_FOUND),
    HIGHLIGHT_CONTENT_OUT_OF_RANGE("HL002", "수집하려는 내용이 해당 문장의 원문 범위를 벗어났습니다.", HttpStatus.BAD_REQUEST),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}
