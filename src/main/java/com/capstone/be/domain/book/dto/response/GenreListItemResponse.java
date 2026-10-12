package com.capstone.be.domain.book.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 장르 전체 목록 조회("GET /api/v1/books/genres", 관리자용) 응답 항목 하나.
 * 책 등록 시 넣을 genreId를 고를 수 있도록 genreId/type/value를 그대로 내려준다.
 */
@Getter
@Builder
@Schema(description = "장르 목록 항목")
public class GenreListItemResponse {

    @Schema(description = "장르 ID (책 등록 시 genreId로 사용)", example = "1")
    private Long genreId;

    @Schema(description = "장르 대분류", example = "소설")
    private String type;

    @Schema(description = "장르 세부 분류", example = "한국소설")
    private String value;
}
