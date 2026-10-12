package com.capstone.be.domain.cutton_page.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "진행 중인 챕터 내용 가져오기"("GET /api/v1/cutton-pages/{pageId}/chapter") 응답.
 * pageId 기준으로 앞뒤 최대 10페이지씩(책 시작/끝이면 있는 만큼만) 조회해서 pages로 내려준다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "기준 페이지 앞뒤 페이지 목록 응답")
public class ChapterContentResponse {

    @Schema(description = "기준 페이지(pageId)가 속한 챕터명 (참고용, 표지 등 챕터 구분이 없으면 null)", example = "1부")
    private String chapter;

    @Schema(description = "이 책의 전체 페이지 수 (마지막 페이지의 pageNumber)", example = "320")
    private Integer allPage;

    @Schema(description = "기준 페이지 앞뒤로 최대 10페이지씩, 실제 존재하는 페이지만 pageNumber 순으로 담은 목록 "
            + "(각 항목에 댓글 수(commentCount, 대댓글 제외)가 포함됩니다)")
    private List<CuttonPageListItemResponse> pages;
}
