package com.capstone.be.domain.book_status.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 책 읽음 상태(회독 기록용). reading_records 하나당(유저+책 조합) 하나씩 같이 생성된다
 * (ReadingRecordService.addBook()). count는 "지금 몇 번째로 읽고 있는 회차인지"를 나타낸다 -
 * 생성 시점엔 1이고, reading_records가 완독 처리될 때마다(1회독→2회독, 2회독→3회독 ...)
 * +1 되면서 last_page_id/progress_percent/last_read_at이 다음 회차를 위해 처음(첫 페이지/0%)
 * 으로 리셋된다.
 * <p>
 * reading_records.completed_at은 최초 완독 시각만 영구히 유지하고 그 뒤로는 바뀌지 않기
 * 때문에, 2회독부터는 이 테이블이 진행 상태(last_page_id/progress_percent/last_read_at)를
 * 대신 추적한다. 독후감 작성 추천/퀴즈 해금 로직은 reading_records 기준(1회독)으로만
 * 동작하고 이 테이블은 건드리지 않는다 - 순수하게 "몇 번 읽었는지/지금 어디까지 읽었는지"
 * 기록용이다.
 */
@Entity
@Table(name = "book_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_status_id")
    private Long bookStatusId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "count")
    private Long count;

    @Column(name = "last_page_id")
    private Long lastPageId;

    @Column(name = "progress_percent", precision = 5, scale = 2)
    private BigDecimal progressPercent;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Builder
    public BookStatus(Long userId, Long bookId, Long count, Long lastPageId,
                       BigDecimal progressPercent, LocalDateTime lastReadAt) {
        this.userId = userId;
        this.bookId = bookId;
        this.count = count;
        this.lastPageId = lastPageId;
        this.progressPercent = progressPercent;
        this.lastReadAt = lastReadAt;
    }

    /** 페이지가 앞으로 나아갔을 때(진행 중인 회차) 호출한다 - ReadingRecord.advanceProgress()와 동일한 패턴. */
    public void advance(Long lastPageId, BigDecimal progressPercent, LocalDateTime readAt) {
        this.lastPageId = lastPageId;
        this.progressPercent = progressPercent;
        this.lastReadAt = readAt;
    }

    /** 진행 상태는 그대로 두고 마지막으로 읽은 시각만 갱신한다(뒤 페이지로 돌아가서 볼 때). */
    public void touchLastReadAt(LocalDateTime readAt) {
        this.lastReadAt = readAt;
    }

    /**
     * 지금 진행 중인 회차를 완료 처리하고, 곧바로 다음 회차 추적을 처음(첫 페이지/0%)부터
     * 시작한다. count가 아직 없으면(레거시 데이터 등) 1부터 시작한 것으로 보고 여기서 +1 한다.
     */
    public void completeCurrentPassAndStartNext(Long firstPageId, LocalDateTime now) {
        this.count = (this.count == null ? 1L : this.count) + 1;
        this.lastPageId = firstPageId;
        this.progressPercent = BigDecimal.ZERO;
        this.lastReadAt = now;
    }
}
