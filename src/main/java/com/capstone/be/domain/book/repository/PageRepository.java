package com.capstone.be.domain.book.repository;

import com.capstone.be.domain.book.entity.Page;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageRepository extends JpaRepository<Page, Long> {

    /**
     * 책 한 권에서 pageNumber가 [startPageNumber, endPageNumber] 구간에 속한 페이지들을
     * 등장 순서대로 가져온다. "진행 중인 챕터 내용 가져오기" API에서 기준 페이지 앞뒤로
     * 최대 10페이지씩 가져올 때 쓴다 - CuttonPageService.getChapterContent() 참고.
     * 책의 시작/끝을 벗어나는 구간은 실제로 존재하는 페이지만 자연스럽게 잘려서 나온다.
     */
    List<Page> findByBook_BookIdAndPageNumberBetweenOrderByPageNumberAsc(
            Long bookId, Integer startPageNumber, Integer endPageNumber);

    /** 책의 마지막 페이지 - 완독 처리 시 last_page_id를 채울 때 쓴다. */
    Optional<Page> findFirstByBook_BookIdOrderByPageNumberDesc(Long bookId);

    /** 책 한 권의 모든 페이지를 페이지 번호 순으로 가져온다. "책 전체 내용 가져오기" API에서 쓴다. */
    List<Page> findByBook_BookIdOrderByPageNumberAsc(Long bookId);

    /**
     * 책 한 권에서 실제 페이지 번호(pageNumber)로 페이지를 찾는다. "진행 중인 챕터 내용
     * 가져오기" API가 pageId 대신 bookId+실제 페이지 번호를 입력받도록 바뀌면서, 기준 페이지를
     * 찾고(+ 존재 확인) 그 chapter를 읽어오는 용도로 쓴다.
     */
    Optional<Page> findByBook_BookIdAndPageNumber(Long bookId, Integer pageNumber);
}
