package com.capstone.be.domain.book.repository;

/**
 * 책 여러 권의 "마지막 페이지 번호(= 전체 페이지 수)"를 배치로 조회할 때 쓰는 프로젝션.
 * 내 도서 목록(ReadingRecordService.getMyBooks())에서 책마다 allPage를 내려줄 때 N+1을 피하기 위해 쓴다.
 */
public interface BookMaxPageNumberProjection {
    Long getBookId();
    Integer getMaxPageNumber();
}
