package com.capstone.be.domain.book.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * "JSON형태 책 저장하기" API의 응답. 저장이 잘 되었는지 확인용 요약 정보만 담는다.
 */
@Getter
@Builder
public class BookCreateResponse {

    private Long bookId;
    private String title;
    private Integer totalPages;
    private Integer savedSentenceCount;
    private Integer savedImageCount;
}
