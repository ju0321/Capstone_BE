package com.capstone.be.domain.cutton_page.service;

import com.capstone.be.domain.book.entity.Page;
import com.capstone.be.domain.book.entity.Sentence;
import com.capstone.be.domain.book.repository.PageRepository;
import com.capstone.be.domain.book.repository.SentenceRepository;
import com.capstone.be.domain.cutton_page.dto.response.CuttonPageResponse;
import com.capstone.be.domain.cutton_page.dto.response.SentenceResponse;
import com.capstone.be.domain.cutton_page.error.CuttonPageErrorCode;
import com.capstone.be.global.exception.CustomException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 책 페이지의 "내용"(문장/이미지 목록)만 담당하는 서비스. 모든 유저가 공유하는 데이터라서
 * pageId 기준으로 Redis에 캐싱한다(CacheConfig 참고).
 * <p>
 * 좋아요/북마크 같은 유저별 상태는 여기서 다루지 않는다(CuttonPageService 참고) - 일부러
 * 별도 빈으로 분리해뒀는데, CuttonPageService가 이 클래스의 getPageContent()를 "외부에서"
 * 호출해야 @Cacheable이 실제로 동작하기 때문이다(같은 빈 안에서 this.getPageContent()처럼
 * 자기 자신을 직접 호출하면 Spring의 프록시 기반 캐싱이 적용되지 않고 매번 DB를 탄다).
 */
@Service
@RequiredArgsConstructor
public class CuttonPageContentService {

    private final PageRepository pageRepository;
    private final SentenceRepository sentenceRepository;

    private static final String CUTTON_PAGE_CACHE = "cuttonPage";

    /**
     * pageId로 책 페이지 내용(문장/이미지 목록)을 조회한다.
     * <p>
     * 문장 목록은 Page를 조회한 뒤 문장을 하나씩 따로 조회하지 않고,
     * SentenceRepository.findByPage_PageIdOrderBySentenceIndexAsc()로 한 번의 쿼리에서
     * 순서대로 전부 가져와 N+1 문제를 피한다.
     * <p>
     * TODO: 관리자가 책 내용을 수정/삭제하는 기능이 생기면, 그 시점에 같은 cacheNames="cuttonPage",
     *  key=pageId로 @CacheEvict를 걸어 캐시를 무효화해야 한다. 안 그러면 수정 후에도 예전 캐시가
     *  TTL 동안 계속 내려간다.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CUTTON_PAGE_CACHE, key = "#pageId")
    public CuttonPageResponse getPageContent(Long pageId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new CustomException(CuttonPageErrorCode.PAGE_NOT_FOUND));

        List<Sentence> sentences = sentenceRepository.findByPage_PageIdOrderBySentenceIndexAsc(pageId);

        List<SentenceResponse> sentenceResponses = sentences.stream()
                .map(sentence -> SentenceResponse.builder()
                        .sentenceId(sentence.getSentenceId())
                        .sentenceIndex(sentence.getSentenceIndex())
                        .content(sentence.getContent())
                        .status(sentence.getStatus())
                        .build())
                .toList();

        return CuttonPageResponse.builder()
                .pageId(page.getPageId())
                .bookId(page.getBook().getBookId())
                .pageNumber(page.getPageNumber())
                .chapter(page.getChapter())
                .imageUrl(page.getImageUrl())
                .sentences(sentenceResponses)
                .build();
    }
}
