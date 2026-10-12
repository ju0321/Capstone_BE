package com.capstone.be.domain.book.repository;

import com.capstone.be.domain.book.entity.Sentence;
import com.capstone.be.domain.book.enums.SentenceStatus;
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
     * 같은 페이지 안에서 특정 인덱스 범위(앞뒤 문장 포함)만 가져온다.
     * 단어 문맥 판별(Lesk)에서 클릭된 문장 하나만이 아니라 앞뒤 문장까지 묶어서
     * 문맥으로 쓰기 위해, 페이지 전체를 불러오지 않고 필요한 범위만 조회한다.
     */
    List<Sentence> findByPage_PageIdAndSentenceIndexBetweenOrderBySentenceIndexAsc(
            Long pageId, Integer fromIndex, Integer toIndex);

    /**
     * 책 한 권의 문장 전체를 페이지/등장 순서대로 가져온다(이미지 항목은 제외).
     * 독후감 퀴즈를 AI로 생성할 때, 책 전체 내용을 문맥으로 넘기기 위해 쓴다.
     */
    @Query("""
            SELECT s FROM Sentence s
            WHERE s.page.book.bookId = :bookId AND s.status = :status
            ORDER BY s.page.pageNumber ASC, s.sentenceIndex ASC
            """)
    List<Sentence> findAllByBookIdAndStatusOrderByPageAndIndex(
            @Param("bookId") Long bookId, @Param("status") SentenceStatus status);

    /**
     * 같은 책 안에서 페이지 번호 범위(from~to, 둘 다 포함) 하나에 속한 문장을 등장 순서대로
     * 가져온다(이미지 항목은 제외). "이해력 증진" 방을 시작할 때, 사용자가 고른 연속된 페이지
     * 범위의 실제 본문을 AI에게 넘겨 첫 질문/선택지를 생성시키기 위해 쓴다
     * (findAllByBookIdAndStatusOrderByPageAndIndex와 같은 모양이지만 책 전체가 아니라
     * 페이지 번호 범위로 좁힌 버전).
     */
    @Query("""
            SELECT s FROM Sentence s
            WHERE s.page.book.bookId = :bookId AND s.status = :status
              AND s.page.pageNumber BETWEEN :fromPageNumber AND :toPageNumber
            ORDER BY s.page.pageNumber ASC, s.sentenceIndex ASC
            """)
    List<Sentence> findAllByBookIdAndPageNumberRangeAndStatusOrderByPageAndIndex(
            @Param("bookId") Long bookId, @Param("fromPageNumber") Integer fromPageNumber,
            @Param("toPageNumber") Integer toPageNumber, @Param("status") SentenceStatus status);

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
