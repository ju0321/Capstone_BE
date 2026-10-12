package com.capstone.be.domain.book_sentences.repository;

import com.capstone.be.domain.book_sentences.entity.SentenceHighlight;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HighlightRepository extends JpaRepository<SentenceHighlight, Long> {

    /** 한 페이지 안의 여러 문장에 대한 유저의 하이라이트를 한 번의 쿼리로 가져온다(N+1 방지). */
    List<SentenceHighlight> findByUserIdAndSentenceIdIn(Long userId, List<Long> sentenceIds);
}
