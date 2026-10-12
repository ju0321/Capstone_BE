package com.capstone.be.domain.reading_record.entity;

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


@Entity
@Table(name = "reading_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "last_page_id", nullable = false)
    private Long lastPageId;

    @Column(name = "progress_rate", precision = 5, scale = 2)
    private BigDecimal progressRate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "quiz_passed_at")
    private LocalDateTime quizPassedAt;

    @Builder
    public ReadingRecord(Long userId, Long bookId, Long lastPageId, BigDecimal progressRate,
                          LocalDateTime createdAt, LocalDateTime lastReadAt, LocalDateTime completedAt) {
        this.userId = userId;
        this.bookId = bookId;
        this.lastPageId = lastPageId;
        this.progressRate = progressRate;
        this.createdAt = createdAt;
        this.lastReadAt = lastReadAt;
        this.completedAt = completedAt;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public boolean isQuizPassed() {
        return quizPassedAt != null;
    }

    public void markQuizPassed(LocalDateTime passedAt) {
        this.quizPassedAt = passedAt;
    }

    /**
     * 페이지를 넘겨서 진행 상황이 실제로 앞으로 나아갔을 때 호출한다. lastPageId/progressRate를
     * 새 값으로 갱신하고 lastReadAt도 같이 갱신한다. 뒤로 돌아가서 이전 페이지를 다시 볼 때는
     * (호출하는 쪽에서 새 페이지 번호가 지금 lastPageId의 페이지 번호보다 클 때만 이 메서드를
     * 불러서) 진행률이 거꾸로 내려가지 않도록 한다 - touchLastReadAt()만 대신 호출한다.
     */
    public void advanceProgress(Long lastPageId, BigDecimal progressRate, LocalDateTime readAt) {
        this.lastPageId = lastPageId;
        this.progressRate = progressRate;
        this.lastReadAt = readAt;
    }

    /** 진행률/마지막 페이지는 그대로 두고 "마지막으로 읽은 시각"만 갱신한다. */
    public void touchLastReadAt(LocalDateTime readAt) {
        this.lastReadAt = readAt;
    }

    /**
     * 페이지 진행을 통해 마지막 페이지까지 다 읽어서 progressRate가 100에 도달했을 때 호출한다.
     * 이미 완독 처리된 적이 있으면(completedAt != null) 최초 완독 시각을 덮어쓰지 않는다 -
     * completeBook()의 upsertCompleted()가 COALESCE로 최초 완독 시각을 유지하는 것과 동일한 정책.
     * 이걸 호출 안 하면 progressRate만 100이고 completedAt은 비어있는 상태가 생겨서, 내 도서
     * 목록(진행률 기준)에서는 완독으로 보이는데 퀴즈(completedAt 기준)는 계속 잠겨있는
     * 불일치가 생긴다.
     */
    public void completeIfNotAlready(LocalDateTime completedAt) {
        if (this.completedAt == null) {
            this.completedAt = completedAt;
        }
    }
}
