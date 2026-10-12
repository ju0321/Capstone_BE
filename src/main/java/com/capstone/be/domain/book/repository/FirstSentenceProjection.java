package com.capstone.be.domain.book.repository;

/**
 * 페이지 하나당 가장 앞(sentence_index가 가장 작은) 문장 하나의 내용을 담는 프로젝션.
 * "나의 북마크/좋아요 목록"에서 페이지 미리보기(첫 문장 한 줄)를 여러 페이지에 대해 한 번의
 * 쿼리로 배치 조회할 때 쓴다(N+1 방지). 이미지 항목(status=IMG)은 제외하고 실제 문장만 대상으로 한다.
 */
public interface FirstSentenceProjection {
    Long getPageId();
    String getContent();
}
