package com.capstone.be.domain.reading_record.repository;

import com.capstone.be.domain.reading_record.entity.ReadingRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReadingRecordRepository extends JpaRepository<ReadingRecord, Long> {

    Optional<ReadingRecord> findByUserIdAndBookId(Long userId, Long bookId);

    /**
     * updateProgress() 전용 - 비관적 락을 걸고 조회한다(BookRepository.findByIdForUpdate()와
     * 동일한 패턴). 같은 유저가 같은 책에 대해 거의 동시에 여러 진행률 갱신 요청을 보내면, 락
     * 없이는 둘 다 "지금 저장된 페이지"를 갱신 전 상태로 읽어서 둘 다 전진으로 판정되고, 나중에
     * 커밋되는 쪽이 먼저 커밋된 더 앞선 진행 상태를 덮어써 진행률이 거꾸로 내려가는 lost update가
     * 생길 수 있다. 조회 시점에 이 행을 잠가서, 동시에 들어온 다른 요청은 먼저 처리 중인
     * 트랜잭션이 끝날 때까지 기다렸다가 갱신된 최신 상태를 보고 판단하게 만든다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReadingRecord r WHERE r.userId = :userId AND r.bookId = :bookId")
    Optional<ReadingRecord> findByUserIdAndBookIdForUpdate(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /** 내 도서 목록에서 삭제하기 API에서 쓴다. */
    void deleteByUserIdAndBookId(Long userId, Long bookId);

    /**
     * 가장 최근에 읽은 책 - lastReadAt이 있는 기록 중 가장 최근 것. "홈 화면 - 최근 읽은 책"
     * API(GET /api/v1/reading-records/current)에서 1순위로 쓴다.
     */
    Optional<ReadingRecord> findFirstByUserIdAndLastReadAtIsNotNullOrderByLastReadAtDesc(Long userId);

    /**
     * 읽다가 만(완독 전) 책들 - progressRate가 0보다 크고 100 미만인 기록을 최근 등록순으로.
     * "홈 화면 - 최근 읽은 책" API에서 lastReadAt이 하나도 없을 때 2순위로 쓴다.
     */
    @Query("""
            SELECT r FROM ReadingRecord r
            WHERE r.userId = :userId
              AND r.progressRate > 0
              AND r.progressRate < 100
            ORDER BY r.createdAt DESC
            """)
    List<ReadingRecord> findInProgress(@Param("userId") Long userId);

    /**
     * 내 도서 목록(무한스크롤, status=ALL). cursor는 바로 이전 응답에서 받은 nextCursor
     * (=그 응답 마지막 항목의 recordId) - 첫 요청이면 null로 보내고, 그 뒤로는 이 값보다 작은
     * (=더 먼저 등록된) 기록부터 이어서 내려준다. recordId는 auto-increment라 생성 순서와
     * 동일하므로, recordId 내림차순 = 최근 등록순이면서 동시에 무한스크롤 cursor로 쓸 수 있을
     * 만큼 값이 단조 증가한다(createdAt은 동시 등록 시 값이 같을 수 있어 cursor로 부적합).
     * pageable은 hasNext 판단을 위해 size+1개를 요청하는 용도로만 쓴다(offset은 항상 0).
     */
    @Query("""
            SELECT r FROM ReadingRecord r
            WHERE r.userId = :userId
              AND (:cursor IS NULL OR r.recordId < :cursor)
            ORDER BY r.recordId DESC
            """)
    List<ReadingRecord> findByUserIdWithCursor(
            @Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * 내 도서 목록(무한스크롤, status=COMPLETED). findByUserIdWithCursor와 동일한 cursor
     * 규칙에 진행률(progressRate >= progressRate) 조건만 추가한 버전이다.
     */
    @Query("""
            SELECT r FROM ReadingRecord r
            WHERE r.userId = :userId
              AND r.progressRate >= :progressRate
              AND (:cursor IS NULL OR r.recordId < :cursor)
            ORDER BY r.recordId DESC
            """)
    List<ReadingRecord> findByUserIdAndProgressRateGreaterThanEqualWithCursor(
            @Param("userId") Long userId, @Param("progressRate") BigDecimal progressRate,
            @Param("cursor") Long cursor, Pageable pageable);

    /**
     * 책을 완독 처리한다(원자적 upsert, HighlightRepository.upsertHighlight()와 동일한 패턴).
     * 이미 완독한 기록이 있으면 completed_at은 최초 완독 시점을 그대로 유지하고
     * (COALESCE), last_page_id/progress_rate/last_read_at만 최신으로 갱신한다.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO reading_records (user_id, book_id, last_page_id, progress_rate, created_at, last_read_at, completed_at)
            VALUES (:userId, :bookId, :lastPageId, 100.00, :now, :now, :now)
            ON CONFLICT (user_id, book_id)
            DO UPDATE SET last_page_id = EXCLUDED.last_page_id,
                          progress_rate = 100.00,
                          last_read_at = EXCLUDED.last_read_at,
                          completed_at = COALESCE(reading_records.completed_at, EXCLUDED.completed_at)
            """, nativeQuery = true)
    void upsertCompleted(@Param("userId") Long userId, @Param("bookId") Long bookId,
                          @Param("lastPageId") Long lastPageId, @Param("now") LocalDateTime now);
}
