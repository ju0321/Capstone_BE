package com.capstone.be.domain.cutton_page.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * "책 페이지 내용 가져오기"("GET /api/v1/cutton-pages/{pageId}") 응답.
 * 이 응답 자체가 CuttonPageService에서 Redis에 캐싱되는 대상이다.
 */
@Getter
@Builder
@Jacksonized
public class CuttonPageResponse {

    private Long pageId;
    private Long bookId;
    private Integer pageNumber;
    private String chapter;
    private String imageUrl;
    private List<SentenceResponse> sentences;
}
