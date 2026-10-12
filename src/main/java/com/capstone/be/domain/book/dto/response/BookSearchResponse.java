package com.capstone.be.domain.book.dto.response;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * 책 검색 API("GET /api/v1/books/search")의 결과 한 건.
 * 표지 이미지/제목/저자/출판사/출판일 등 목록에 보여줄 정보만 담는다.
 */
@Getter
@Builder
public class BookSearchResponse {

    private Long bookId;
    private String title;
    private String author;
    private String publisher;
    private String coverImageUrl;
    private LocalDate createdAt;
}
