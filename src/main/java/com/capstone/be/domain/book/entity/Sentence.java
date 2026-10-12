package com.capstone.be.domain.book.entity;

import com.capstone.be.domain.book.enums.SentenceStatus;
import com.capstone.be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sentences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sentence extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sentence_id")
    private Long sentenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    // 페이지 안에서의 등장 순서 (문장/이미지 공통으로 사용)
    @Column(name = "sentence_index")
    private Integer sentenceIndex;

    // status가 SENTENCE면 문장 텍스트, IMG면 이미지 URL이 들어간다.
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SentenceStatus status;

    @Builder
    public Sentence(Page page, Integer sentenceIndex, String content, SentenceStatus status) {
        this.page = page;
        this.sentenceIndex = sentenceIndex;
        this.content = content;
        this.status = status;
    }
}
