package com.capstone.be.domain.book_sentences.repository;

import java.time.LocalDateTime;

/**
 * HighlightRepository.findAllByUserId()의 조회 결과 프로젝션.
 * highlights(sentence_id만 가짐)에 sentences -> cutton_pages -> books를 조인해서
 * 어느 책 몇 페이지의 문장인지까지 한 번의 쿼리로 가져오기 위한 인터페이스 프로젝션이다.
 * 네이티브 쿼리의 컬럼 별칭(AS sentenceId 등)과 아래 getter 이름이 매칭되어야 한다.
 */
public interface HighlightListProjection {

    Long getSentenceId();

    String getContent();

    String getColor();

    LocalDateTime getCreatedAt();

    Long getBookId();

    String getBookTitle();

    Long getPageId();

    Integer getPageNumber();

    String getChapter();
}
