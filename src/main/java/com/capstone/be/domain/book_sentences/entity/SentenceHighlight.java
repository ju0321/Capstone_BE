package com.capstone.be.domain.book_sentences.entity;

import com.capstone.be.domain.book_sentences.enums.HighlightColor;
import com.capstone.be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저가 책을 읽다가 드래그로 선택해서 수집한 문장(형광펜 하이라이트).
 * sentence_id로 sentences 테이블의 문장을 직접 참조한다(드래그 선택 범위가 문장 하나를
 * 벗어나지 않는다는 전제). content는 선택한 문장 원문 스냅샷 - sentence.content가 나중에
 * 바뀌더라도 유저가 수집한 시점의 문장이 그대로 남아 있도록 별도로 저장한다.
 */
@Entity
@Table(name = "highlights")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SentenceHighlight extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "highlight_id")
    private Long highlightId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sentence_id", nullable = false)
    private Long sentenceId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false, length = 20)
    private HighlightColor color;

    @Builder
    public SentenceHighlight(Long userId, Long sentenceId, String content, HighlightColor color) {
        this.userId = userId;
        this.sentenceId = sentenceId;
        this.content = content;
        this.color = color;
    }
}
