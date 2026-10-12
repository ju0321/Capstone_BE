package com.capstone.be.domain.book.error;

import com.capstone.be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookErrorCode implements BaseErrorCode {

    INVALID_BOOK_JSON("B001", "book_Json 파일을 읽을 수 없습니다. 형식을 확인해주세요.", HttpStatus.BAD_REQUEST),
    BOOK_NOT_FOUND("B002", "존재하지 않는 책입니다.", HttpStatus.NOT_FOUND),
    SENTENCE_NOT_FOUND("B003", "존재하지 않는 문장입니다.", HttpStatus.NOT_FOUND),
    GENRE_NOT_FOUND("B004", "존재하지 않는 장르입니다.", HttpStatus.NOT_FOUND),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}
