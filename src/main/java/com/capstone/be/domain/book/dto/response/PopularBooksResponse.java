package com.capstone.be.domain.book.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * "인기 도서" 목록 API의 응답. 20권씩 페이징해서 보여주기 위한 페이지 정보도 함께 담는다.
 * (지금은 실제 인기도 데이터가 없어서 무작위 순서로 채워짐 - BookRepository.findRandomBooks() 참고)
 */
@Getter
@Builder
public class PopularBooksResponse {

    private List<BookSearchResponse> books;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}
