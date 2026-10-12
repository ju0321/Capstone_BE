package com.capstone.be.domain.cutton_page.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "나의 북마크 목록"/"나의 좋아요 목록"(전체 탭) 응답 - 무한스크롤(커서 기반) 형태다.
 * 정렬 기준이 등록순(단일 PK)이 아니라 "저장한 날짜"(다시 켜면 갱신됨)이라, 커서가
 * nextCursorSavedAt/nextCursorId 2개 값의 조합이다 - 다음 요청에는 이 둘을 함께 그대로 보낸다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "북마크/좋아요한 페이지 목록 응답 (무한스크롤 커서 기반)")
public class PageStatusListResponse {

    @Schema(description = "목록 항목")
    private List<PageStatusListItemResponse> items;

    @Schema(description = "다음 요청 시 cursorSavedAt으로 보낼 값. hasNext가 false면 null")
    private LocalDate nextCursorSavedAt;

    @Schema(description = "다음 요청 시 cursorId로 보낼 값(nextCursorSavedAt과 함께 사용). hasNext가 false면 null",
            example = "42")
    private Long nextCursorId;

    @Schema(description = "다음 페이지(더 가져올 목록) 존재 여부")
    private boolean hasNext;
}
