package com.capstone.be.domain.book.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 책 상세정보("GET /api/v1/books/{bookId}") 응답에 담기는 장르 항목 하나.
 * Book.genreId로 genres 테이블에서 조회한 type/value를 그대로 담는다.
 */
@Getter
@Builder
@Schema(description = "장르 항목")
public class GenreResponse {

    @Schema(description = "장르 대분류", example = "소설")
    private String type;

    @Schema(description = "장르 세부 분류", example = "한국소설")
    private String value;
}
