package com.capstone.be.domain.reading_record.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 내 도서 목록 조회(GET /api/v1/reading-records) 응답 - 무한스크롤(커서 기반) 형태다.
 * items가 비어있거나 hasNext가 false면 더 가져올 목록이 없다는 뜻이고, hasNext가 true면
 * nextCursor를 다음 요청의 cursor 파라미터로 그대로 보내면 이어서 다음 목록을 받는다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 도서 목록 조회 응답 (무한스크롤 커서 기반)")
public class ReadingRecordListResponse {

    @Schema(description = "도서 목록 항목")
    private List<ReadingRecordListItemResponse> items;

    @Schema(description = "다음 요청 시 cursor로 보낼 값. hasNext가 false면 null", example = "42")
    private Long nextCursor;

    @Schema(description = "다음 페이지(더 가져올 목록) 존재 여부")
    private boolean hasNext;
}
