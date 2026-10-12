package com.capstone.be.domain.book.repository;

import com.capstone.be.domain.book.entity.Book;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.bookId = :bookId")
    Optional<Book> findByIdForUpdate(@Param("bookId") Long bookId);

    /**
     * 제목/저자/출판사 중 하나라도 keyword를 포함하는 책을 관련도순으로 정렬해서 반환한다.
     * 관련도 우선순위: 제목 시작일치 -> 제목 포함일치 -> 저자 시작일치 -> 저자 포함일치
     * -> 출판사 시작일치 -> 출판사 포함일치.
     */
    @Query("""
            SELECT b FROM Book b
            WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\'
               OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\'
               OR LOWER(b.publisher) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\'
            ORDER BY
                CASE
                    WHEN LOWER(b.title) LIKE LOWER(CONCAT(:keyword, '%')) ESCAPE '\\' THEN 0
                    WHEN LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' THEN 1
                    WHEN LOWER(b.author) LIKE LOWER(CONCAT(:keyword, '%')) ESCAPE '\\' THEN 2
                    WHEN LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' THEN 3
                    WHEN LOWER(b.publisher) LIKE LOWER(CONCAT(:keyword, '%')) ESCAPE '\\' THEN 4
                    ELSE 5
                END,
                b.title ASC
            """)
    List<Book> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * "인기 도서" 목록. 아직 조회수/좋아요 같은 실제 인기도 데이터가 없어서,
     * 임시로 전체 책 중에서 무작위 순서로 페이징해서 보여준다.
     * TODO: viewCount 등 실제 인기도 지표가 쌓이면 ORDER BY RANDOM() 대신
     *  그 지표 기준 정렬로 교체해야 한다.
     */
    @Query(value = "SELECT * FROM books ORDER BY RANDOM()",
            countQuery = "SELECT COUNT(*) FROM books",
            nativeQuery = true)
    Page<Book> findRandomBooks(Pageable pageable);

    /**
     * "취향 추천도서" API용. 지정한 bookId들(보통 내가 이미 가지고 있는 책)을 제외한 책 중에서
     * 무작위로 limit개를 가져온다.
     * TODO: 지금은 취향 데이터가 없어서 임시로 랜덤 추천이다. 추후 유저의 취향(장르/카테고리 등)에
     *  맞는 책을 기준으로 10권을 가져오도록 교체해야 한다.
     */
    @Query(value = "SELECT * FROM books WHERE book_id NOT IN (:excludedBookIds) ORDER BY RANDOM() LIMIT :limit",
            nativeQuery = true)
    List<Book> findRandomBooksExcluding(@Param("excludedBookIds") List<Long> excludedBookIds,
                                         @Param("limit") int limit);

    /**
     * "마지막으로 읽은 책 기반 추천도서" API용. 장르 대분류(genres.type)가 일치하는 책 중에서
     * 무작위로 limit개를 가져온다. (내가 이미 가지고 있는 책이 하나도 없어서 제외할 게 없을 때
     * findRandomBooksByGenreTypeExcluding 대신 쓴다 - 빈 IN절은 네이티브 쿼리에서 바인딩
     * 에러가 나기 때문에 나눠뒀다.)
     */
    @Query(value = """
            SELECT b.* FROM books b
            JOIN genres g ON g.genre_id = b.genre_id
            WHERE g.type = :type
            ORDER BY RANDOM() LIMIT :limit
            """, nativeQuery = true)
    List<Book> findRandomBooksByGenreType(@Param("type") String type, @Param("limit") int limit);

    /** findRandomBooksByGenreType()와 동일하지만, 지정한 bookId들(보통 내가 이미 가지고 있는 책)을 제외한다. */
    @Query(value = """
            SELECT b.* FROM books b
            JOIN genres g ON g.genre_id = b.genre_id
            WHERE g.type = :type AND b.book_id NOT IN (:excludedBookIds)
            ORDER BY RANDOM() LIMIT :limit
            """, nativeQuery = true)
    List<Book> findRandomBooksByGenreTypeExcluding(@Param("type") String type,
                                                    @Param("excludedBookIds") List<Long> excludedBookIds,
                                                    @Param("limit") int limit);
}
