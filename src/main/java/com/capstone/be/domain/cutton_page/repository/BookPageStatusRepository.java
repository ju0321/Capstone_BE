package com.capstone.be.domain.cutton_page.repository;

import com.capstone.be.domain.cutton_page.entity.BookPageStatus;
import com.capstone.be.domain.cutton_page.enums.PageReactionType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookPageStatusRepository extends JpaRepository<BookPageStatus, Long> {

    /** 이 유저가 이 페이지에 대해 가진 반응(북마크/좋아요) 전체. 0~2개(타입당 최대 1개)가 나온다. */
    List<BookPageStatus> findByUserIdAndPageId(Long userId, Long pageId);

    /**
     * 이 페이지에 대한 전체 유저의 반응(좋아요/북마크) 개수. "책 페이지 내용 가져오기"
     * (CuttonPageService.getPageContentForUser())에서 이 페이지의 좋아요 수를 같이 내려줄 때 쓴다.
     */
    long countByPageIdAndType(Long pageId, PageReactionType type);

    /**
     * 북마크/좋아요를 켠다. (page_id, user_id, type) 유니크 제약 덕분에 이미 켜져 있으면
     * INSERT ... ON CONFLICT DO NOTHING으로 조용히 무시된다(멱등) - 이미 켜진 상태에서 또
     * 요청이 와도 created_at이 바뀌지 않는다("한 번도 취소 안 한 채로 다시 누른 것"은 사용자
     * 입장에서 새로 저장한 게 아니라서). 동시에 같은 요청이 여러 번 와도 경쟁 상태 없이 안전하다.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO book_page_status (user_id, page_id, type, created_at)
            VALUES (:userId, :pageId, :type, CURRENT_DATE)
            ON CONFLICT (page_id, user_id, type) DO NOTHING
            """, nativeQuery = true)
    void insertReaction(@Param("userId") Long userId, @Param("pageId") Long pageId, @Param("type") String type);

    /**
     * 북마크/좋아요를 끈다(row 삭제). CommentReactionRepository.deleteByComment_CommentIdAndUser_UserIdAndType()와
     * 동일한 패턴 - 반응이 없었어도 조용히 0건 삭제로 끝난다(멱등한 취소). 취소 후 다시 켜면
     * insertReaction()이 새 row를 만들면서 created_at이 그 시점으로 자연스럽게 갱신된다(별도
     * 갱신 로직 없이 "다시 누르면 최신으로 올라옴" 정책이 만족됨).
     */
    @Modifying(clearAutomatically = true)
    void deleteByUserIdAndPageIdAndType(Long userId, Long pageId, PageReactionType type);

    /**
     * 이 유저가 이 책의 페이지들에 대해 가진 반응(북마크/좋아요)을 전부 지운다. 내 도서 목록에서
     * 책을 삭제할 때(ReadingRecordService.deleteBook()) 같이 호출한다 - book_page_status는
     * user_id/page_id로만 연결돼 있어서 reading_records를 지워도 DB가 자동으로 지워주지 않는다.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            DELETE FROM book_page_status
            WHERE user_id = :userId
              AND page_id IN (SELECT page_id FROM cutton_pages WHERE book_id = :bookId)
            """, nativeQuery = true)
    void deleteByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /**
     * 나의 북마크/좋아요 목록(무한스크롤) - type으로 걸러서 created_at 최신순. created_at이
     * DATE라 같은 날짜인 항목이 많이 겹칠 수 있어서, cursorSavedAt(날짜)만으로는 순서를 보장할
     * 수 없어 recordPageId를 2차 키로 같이 쓴다. 첫 요청은 둘 다 null로 보낸다.
     */
    @Query("""
            SELECT b FROM BookPageStatus b
            WHERE b.userId = :userId AND b.type = :type
              AND (:cursorSavedAt IS NULL
                   OR b.createdAt < :cursorSavedAt
                   OR (b.createdAt = :cursorSavedAt AND b.recordPageId < :cursorId))
            ORDER BY b.createdAt DESC, b.recordPageId DESC
            """)
    List<BookPageStatus> findByUserIdAndTypeWithCursor(
            @Param("userId") Long userId, @Param("type") PageReactionType type,
            @Param("cursorSavedAt") LocalDate cursorSavedAt, @Param("cursorId") Long cursorId, Pageable pageable);
}
