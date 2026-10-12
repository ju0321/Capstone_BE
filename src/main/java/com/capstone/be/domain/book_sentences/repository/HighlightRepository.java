package com.capstone.be.domain.book_sentences.repository;

import com.capstone.be.domain.book_sentences.entity.SentenceHighlight;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HighlightRepository extends JpaRepository<SentenceHighlight, Long> {

    Optional<SentenceHighlight> findByUserIdAndSentenceId(Long userId, Long sentenceId);

    /** 마이페이지 "수집한 문장 수" 통계용. */
    long countByUserId(Long userId);

    /** 한 페이지 안의 여러 문장에 대한 유저의 하이라이트를 한 번의 쿼리로 가져온다(N+1 방지). */
    List<SentenceHighlight> findByUserIdAndSentenceIdIn(Long userId, List<Long> sentenceIds);

    /**
     * (user_id, sentence_id) row가 없으면 새로 만들고, 있으면 color/content만 갱신한다.
     * INSERT ... ON CONFLICT 하나로 끝나는 원자적 연산이라 "조회 후 분기"할 때 생기는
     * 경쟁 상태가 없다. (BookPageStatusRepository.insertReaction()와 동일한 패턴)
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO highlights (user_id, sentence_id, color, content, created_at, updated_at)
            VALUES (:userId, :sentenceId, :color, :content, NOW(), NOW())
            ON CONFLICT (user_id, sentence_id)
            DO UPDATE SET color = EXCLUDED.color, content = EXCLUDED.content, updated_at = NOW()
            """, nativeQuery = true)
    void upsertHighlight(@Param("userId") Long userId, @Param("sentenceId") Long sentenceId,
                          @Param("color") String color, @Param("content") String content);

    /**
     * 문장 하나에 대한 유저의 하이라이트를 삭제한다(있으면 지우고, 없어도 에러 없이 0건 처리).
     */
    void deleteByUserIdAndSentenceId(Long userId, Long sentenceId);

    /**
     * 이 유저가 이 책에서 쓴 하이라이트를 전부 지운다. 내 도서 목록에서 책을 삭제할 때
     * (ReadingRecordService.deleteBook()) 같이 호출한다 - highlights는 user_id/sentence_id로만
     * 연결돼 있어서 reading_records를 지워도 DB가 자동으로 지워주지 않는다.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            DELETE FROM highlights
            WHERE user_id = :userId
              AND sentence_id IN (
                  SELECT s.sentence_id FROM sentences s
                  JOIN cutton_pages p ON p.page_id = s.page_id
                  WHERE p.book_id = :bookId
              )
            """, nativeQuery = true)
    void deleteByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /**
     * 이미 있는 하이라이트의 색상만 갱신한다(content는 건드리지 않음). 존재 여부는 서비스에서
     * findByUserIdAndSentenceId()로 먼저 확인하고 호출하므로, 여기서는 갱신만 원자적으로 한다.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE highlights SET color = :color, updated_at = NOW()
            WHERE user_id = :userId AND sentence_id = :sentenceId
            """, nativeQuery = true)
    void updateColor(@Param("userId") Long userId, @Param("sentenceId") Long sentenceId,
                      @Param("color") String color);

    /**
     * 유저가 수집한 문장 목록을, 어느 책 몇 페이지의 문장인지까지 같이 조회한다(최신순).
     * highlights는 sentence_id만 갖고 있어서 sentences -> cutton_pages -> books를 조인해야
     * bookTitle/pageNumber를 알 수 있는데, SentenceHighlight 엔티티가 Sentence/Page/Book과
     * 매핑된 연관관계 없이 raw FK 컬럼만 갖고 있어서(BookPageStatus와 동일한 설계) JPQL 조인이
     * 안 되므로 네이티브 쿼리로 처리한다. bookId를 안 주면(null) 전체 책 대상으로 조회한다.
     */
    @Query(value = """
            SELECT h.sentence_id AS sentenceId,
                   h.content     AS content,
                   h.color       AS color,
                   h.created_at  AS createdAt,
                   b.book_id     AS bookId,
                   b.title       AS bookTitle,
                   p.page_id     AS pageId,
                   p.page_number AS pageNumber,
                   p.chapter     AS chapter
            FROM highlights h
            JOIN sentences s ON s.sentence_id = h.sentence_id
            JOIN cutton_pages p ON p.page_id = s.page_id
            JOIN books b ON b.book_id = p.book_id
            WHERE h.user_id = :userId
              AND (:bookId IS NULL OR b.book_id = :bookId)
            ORDER BY h.created_at DESC
            """, nativeQuery = true)
    List<HighlightListProjection> findAllByUserId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /** 수집한 문장 하나의 상세보기(책/페이지 컨텍스트 포함)를 조회한다. */
    @Query(value = """
            SELECT h.sentence_id AS sentenceId,
                   h.content     AS content,
                   h.color       AS color,
                   h.created_at  AS createdAt,
                   b.book_id     AS bookId,
                   b.title       AS bookTitle,
                   p.page_id     AS pageId,
                   p.page_number AS pageNumber,
                   p.chapter     AS chapter
            FROM highlights h
            JOIN sentences s ON s.sentence_id = h.sentence_id
            JOIN cutton_pages p ON p.page_id = s.page_id
            JOIN books b ON b.book_id = p.book_id
            WHERE h.user_id = :userId AND h.sentence_id = :sentenceId
            """, nativeQuery = true)
    Optional<HighlightListProjection> findDetailByUserIdAndSentenceId(
            @Param("userId") Long userId, @Param("sentenceId") Long sentenceId);
}
