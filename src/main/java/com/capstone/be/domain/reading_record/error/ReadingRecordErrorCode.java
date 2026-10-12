package com.capstone.be.domain.reading_record.error;

import com.capstone.be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReadingRecordErrorCode implements BaseErrorCode {

    READING_RECORD_ALREADY_EXISTS("BR009", "이미 내 도서 목록에 추가된 책입니다.", HttpStatus.CONFLICT),
    READING_RECORD_NOT_FOUND("BR010", "존재하지 않거나 접근 권한이 없는 독서 기록입니다.", HttpStatus.NOT_FOUND),
    PAGE_BOOK_MISMATCH("BR011", "해당 페이지는 이 독서 기록의 책에 속해있지 않습니다.", HttpStatus.BAD_REQUEST),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}
