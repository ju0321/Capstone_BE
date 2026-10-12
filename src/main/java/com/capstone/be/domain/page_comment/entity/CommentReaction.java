package com.capstone.be.domain.page_comment.entity;

import com.capstone.be.domain.page_comment.enums.ReactionType;
import com.capstone.be.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 댓글에 대한 사용자 반응(좋아요/싫어요)을 나타낸다.
 * 한 유저는 하나의 댓글에 대해 반응을 하나만 가질 수 있다(좋아요와 싫어요를 동시에 가질 수 없음).
 * 그래서 좋아요 <-> 싫어요 전환은 기존 row의 type을 바꾸는 방식으로 처리하고,
 * 반응 취소는 row 자체를 삭제하는 방식으로 처리한다.
 */
@Entity
@Table(name = "comment_reactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comment_reaction_comment_user",
                columnNames = {"comment_id", "user_id"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class CommentReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_reaction_id")
    private Long commentReactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private PageComment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ReactionType type;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public CommentReaction(PageComment comment, User user, ReactionType type) {
        this.comment = comment;
        this.user = user;
        this.type = type;
    }
}
