package com.capstone.be.domain.page_comment.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 댓글/답글 목록 조회 API 응답.
 * - 최신순(LATEST)/답글: 무한스크롤 - comments/nextCursor/hasNext만 의미 있고, page/size/
 *   totalElements/totalPages는 채워지지 않는다(기본값).
 * - 인기순(POPULAR): 기존과 동일한 offset 페이징 - page/size/totalElements/totalPages/hasNext가
 *   채워지고 nextCursor는 항상 null이다.
 */
@Getter
@Builder
public class PageCommentListResponse {

    private List<PageCommentResponse> comments;
    private Long nextCursor;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}
