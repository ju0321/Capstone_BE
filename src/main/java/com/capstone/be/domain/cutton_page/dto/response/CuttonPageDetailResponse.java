package com.capstone.be.domain.cutton_page.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * "책 페이지 내용 가져오기"("GET /api/v1/cutton-pages/{pageId}") 최종 응답.
 * <p>
 * 콘텐츠(pageId~sentences)는 CuttonPageContentService에서 pageId 기준으로 Redis에 캐싱된
 * CuttonPageResponse를 그대로 재사용하고, bookmarked/liked는 유저마다 다른 값이라 캐싱하지
 * 않고 요청마다 BookPageStatusRepository에서 가볍게 조회해서 그 위에 얹는다. likeCount/
 * commentCount도 전체 유저 기준 집계값이라 마찬가지로 매 요청마다 COUNT 쿼리로 조회한다.
 * 즉 이 클래스 자체는 캐시에 저장되는 대상이 아니라, 매 요청마다 새로 조립되는 응답이다.
 */
@Getter
@Builder
public class CuttonPageDetailResponse {

    private Long pageId;
    private Long bookId;
    private Integer pageNumber;
    private Integer allPage;
    private String chapter;
    private String imageUrl;
    private List<SentenceResponse> sentences;
    private boolean bookmarked;
    private boolean liked;
    private long likeCount;
    private long commentCount;
}
