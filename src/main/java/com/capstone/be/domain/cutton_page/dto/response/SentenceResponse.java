package com.capstone.be.domain.cutton_page.dto.response;

import com.capstone.be.domain.book.enums.SentenceStatus;
import com.capstone.be.domain.book_sentences.enums.HighlightColor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * 페이지 안의 문장(또는 이미지) 한 건.
 * status가 SENTENCE면 content는 문장 텍스트, IMG면 content는 이미지 URL이다.
 * <p>
 * highlightColor는 이 문장을 조회한 유저가 형광펜으로 수집(하이라이트)해뒀다면 그 색상,
 * 아니면 null이다. 유저마다 다른 값이라 CuttonPageContentService가 Redis에 캐싱하는
 * 원본 응답에는 항상 null로 들어가고, CuttonPageService.getPageContentForUser()에서
 * 유저별로 다시 조립할 때 실제 값이 채워진다(bookmarked/liked와 같은 이유).
 */
@Getter
@Builder
@Jacksonized
public class SentenceResponse {

    private Long sentenceId;
    private Integer sentenceIndex;
    private String content;
    private SentenceStatus status;
    private HighlightColor highlightColor;
}
