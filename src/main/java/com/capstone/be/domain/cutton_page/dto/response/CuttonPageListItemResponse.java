package com.capstone.be.domain.cutton_page.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * "진행 중인 챕터 내용 가져오기"/"책 전체 내용 가져오기" 응답(ChapterContentResponse.pages)의
 * 항목 하나. 콘텐츠(pageId~sentences)는 CuttonPageContentService에서 Redis에 캐싱된
 * CuttonPageResponse를 그대로 재사용하고, commentCount는 좋아요 수와 같은 이유로(전체 유저
 * 기준 집계값이라 캐시에 넣지 않고 매번 새로 계산) 캐시된 콘텐츠 위에 얹어서 내려준다
 * (CuttonPageDetailResponse와 동일한 원칙). 대댓글은 세지 않고 최상위 댓글만 센다.
 */
@Getter
@Builder
@Schema(description = "책 페이지 내용 한 건 (댓글 수 포함)")
public class CuttonPageListItemResponse {

    private Long pageId;
    private Long bookId;
    private Integer pageNumber;
    private String chapter;
    private String imageUrl;
    private List<SentenceResponse> sentences;

    @Schema(description = "이 페이지의 댓글 수 (대댓글 제외, 최상위 댓글만)", example = "3")
    private long commentCount;
}
