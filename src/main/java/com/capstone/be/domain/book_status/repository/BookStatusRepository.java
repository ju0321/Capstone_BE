package com.capstone.be.domain.book_status.repository;

import com.capstone.be.domain.book_status.entity.BookStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookStatusRepository extends JpaRepository<BookStatus, Long> {

    Optional<BookStatus> findByUserIdAndBookId(Long userId, Long bookId);

    /** 내 도서 목록 응답에 회독 수(count)를 책별로 같이 내려주기 위한 배치 조회(N+1 방지). */
    List<BookStatus> findByUserIdAndBookIdIn(Long userId, List<Long> bookIds);

    /**
     * ReadingRecordService의 회독 진행/완료 처리 전용 - 비관적 락을 걸고 조회한다
     * (ReadingRecordRepository.findByUserIdAndBookIdForUpdate()와 동일한 패턴). 같은 유저가
     * 같은 책에 대해 거의 동시에 진행 상태를 갱신하면 lost update가 생길 수 있어서, 조회
     * 시점에 이 행을 잠가 동시 요청을 직렬화한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BookStatus s WHERE s.userId = :userId AND s.bookId = :bookId")
    Optional<BookStatus> findByUserIdAndBookIdForUpdate(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /** 내 도서 목록에서 책을 삭제할 때 같이 지운다(reading_records와 직접 FK로 연결돼 있지 않음). */
    void deleteByUserIdAndBookId(Long userId, Long bookId);
}
