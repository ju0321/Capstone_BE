package com.capstone.be.domain.cutton_page.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** "나의 북마크 목록"/"나의 좋아요 목록"(전체 탭) 항목 하나. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "북마크/좋아요한 페이지 목록 항목 하나")
public class PageStatusListItemResponse {

    @Schema(description = "페이지 ID (GET /api/v1/cutton-pages/{pageId}로 이동할 때 사용)", example = "101")
    private Long pageId;

    @Schema(description = "책 ID", example = "1")
    private Long bookId;

    @Schema(description = "책 제목", example = "양면의 조개껍데기")
    private String bookTitle;

    @Schema(description = "책 표지 이미지 URL")
    private String coverImageUrl;

    @Schema(description = "실제 책 기준 페이지 번호", example = "58")
    private Integer pageNumber;

    @Schema(description = "해당 페이지의 가장 앞 문장 한 줄 (문장이 없으면 null)")
    private String firstSentence;

    @Schema(description = "북마크/좋아요를 저장한(마지막으로 켠) 날짜")
    private LocalDate savedAt;
}
