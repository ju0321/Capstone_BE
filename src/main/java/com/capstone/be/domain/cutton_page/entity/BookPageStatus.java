package com.capstone.be.domain.cutton_page.entity;

import com.capstone.be.domain.cutton_page.enums.PageReactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저 한 명이 페이지 하나에 대해 가진 반응(북마크 또는 좋아요) 하나. comment_reactions와
 * 같은 "행 하나 = 반응 하나" 패턴이지만, 북마크/좋아요는 서로 배타적이지 않아(둘 다 가능)
 * 유니크 제약이 (comment_id, user_id) 2컬럼이 아니라 (page_id, user_id, type) 3컬럼이다 -
 * 그래서 같은 유저가 같은 페이지에 대해 BOOKMARK 행 + LIKE 행을 동시에 가질 수 있다.
 * <p>
 * 생성/삭제는 BookPageStatusRepository.insertReaction()/deleteByUserIdAndPageIdAndType()로
 * 처리한다(엔티티를 조회해서 필드를 바꾸는 방식이 아님) - 그래서 이 클래스는 읽기(조회 응답
 * 조립)용으로만 쓰이고 별도의 상태 변경 메서드는 두지 않는다.
 */
@Entity
@Table(name = "book_page_status",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_book_page_status_page_user_type", columnNames = {"page_id", "user_id", "type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookPageStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_page_id")
    private Long recordPageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "page_id", nullable = false)
    private Long pageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PageReactionType type;

    /** 이 반응을 남긴(북마크/좋아요를 켠) 날짜. 취소 후 다시 켜면 새 row로 다시 채워진다. */
    @Column(name = "created_at")
    private LocalDate createdAt;

    @Builder
    public BookPageStatus(Long userId, Long pageId, PageReactionType type, LocalDate createdAt) {
        this.userId = userId;
        this.pageId = pageId;
        this.type = type;
        this.createdAt = createdAt;
    }
}
