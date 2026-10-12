package com.capstone.be.domain.book.repository;

import com.capstone.be.domain.book.entity.Sentence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {

    /**
     * 특정 페이지에 속한 문장/이미지를 등장 순서(sentenceIndex)대로 한 번의 쿼리로 가져온다.
     * (Page를 조회한 뒤 문장을 하나씩 따로 조회하는 N+1 대신, 이 메서드 하나로 페이지의
     * 문장 전체를 한 번에 가져와서 CuttonPageService에서 사용한다.)
     */
    List<Sentence> findByPage_PageIdOrderBySentenceIndexAsc(Long pageId);

    /**
     * 주어진 페이지 id들 각각에 대해 가장 앞(sentence_index가 가장 작은) 문장 하나씩의 내용만
     * 한 번의 쿼리로 배치 조회한다("나의 북마크/좋아요 목록" 미리보기용). 페이지마다 따로
     * LIMIT 1 쿼리를 날리는 N+1을 피하기 위해 Postgres의 DISTINCT ON을 쓴다 - JPQL이
     * 지원하지 않는 문법이라 nativeQuery로 작성했다. 이미지 항목(status='IMG')은 제외한다.
     */
    @Query(value = """
            SELECT DISTINCT ON (s.page_id) s.page_id AS pageId, s.content AS content
            FROM sentences s
            WHERE s.page_id IN (:pageIds) AND s.status = 'SENTENCE'
            ORDER BY s.page_id, s.sentence_index ASC
            """, nativeQuery = true)
    List<FirstSentenceProjection> findFirstSentenceContentByPageIds(@Param("pageIds") List<Long> pageIds);
}
