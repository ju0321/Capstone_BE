package com.capstone.be.domain.page_comment.entity;

import com.capstone.be.domain.book.entity.Page;
import com.capstone.be.domain.book.entity.Sentence;
import com.capstone.be.domain.page_comment.enums.PageCommentStatus;
import com.capstone.be.domain.user.entity.User;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

@Entity
@Table(name = "page_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PageComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    // 어느 문장에 달린 댓글인지. sentenceId 없이 작성하면 특정 문장이 아니라 페이지 전체에
    // 달린 댓글이라는 뜻이라 null 허용(V20 마이그레이션).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sentence_id", nullable = true)
    private Sentence sentence;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PageCommentStatus status;

    // 답글인 경우 원댓글을 가리킨다. 최상위 댓글이면 부모가 없으므로 null 허용.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", nullable = true)
    private PageComment parentComment;

    @Builder
    public PageComment(User user, Page page, Sentence sentence, String content,
                        PageCommentStatus status, PageComment parentComment) {
        this.user = user;
        this.page = page;
        this.sentence = sentence;
        this.content = content;
        this.status = status;
        this.parentComment = parentComment;
    }

    // 댓글 내용을 수정한다. (작성자 본인 여부 검증은 서비스 계층에서 이미 마친 뒤 호출된다고 가정)
    // status(스포일러 여부)는 클라이언트가 판단할 수 없는 값이라 여기서 건드리지 않는다.
    // (추후 AI가 content를 다시 판단해서 상태를 바꾸는 로직은 별도 메서드로 추가될 예정)
    public void update(String content) {
        this.content = content;
    }
}
