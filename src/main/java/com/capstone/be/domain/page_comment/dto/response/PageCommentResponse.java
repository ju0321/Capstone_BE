package com.capstone.be.domain.page_comment.dto.response;

import com.capstone.be.domain.page_comment.entity.PageComment;
import com.capstone.be.domain.page_comment.enums.PageCommentStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PageCommentResponse {

    private Long commentId;
    private Long pageId;
    private Long sentenceId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String content;
    private PageCommentStatus status;
    private LocalDateTime createdAt;
    // 답글이면 원댓글 id, 최상위 댓글이면 null
    private Long parentCommentId;
    private long likeCount;
    private long dislikeCount;
    // 이 댓글에 달린 답글 수. 답글 자신에 대해서는 항상 0이다(답글에는 답글을 달 수 없음).
    private long replyCount;

    // 새로 작성된 댓글/답글처럼 반응/답글이 있을 수 없는 경우(좋아요/싫어요/답글 0개 확정)에 사용한다.
    public static PageCommentResponse from(PageComment comment) {
        return from(comment, 0L, 0L, 0L);
    }

    // 연관관계(user/page/sentence/parentComment)가 LAZY라서, 트랜잭션이 열려있는 서비스 메서드 안에서 호출해야 한다.
    // likeCount/dislikeCount/replyCount는 각각 CommentReactionRepository/PageCommentRepository로
    // 미리 집계해서 넘겨받는다(N+1 방지).
    public static PageCommentResponse from(PageComment comment, long likeCount, long dislikeCount, long replyCount) {
        return PageCommentResponse.builder()
                .commentId(comment.getCommentId())
                .pageId(comment.getPage().getPageId())
                .sentenceId(comment.getSentence() != null ? comment.getSentence().getSentenceId() : null)
                .userId(comment.getUser().getUserId())
                .nickname(comment.getUser().getNickname())
                .profileImageUrl(comment.getUser().getProfileImageUrl())
                .content(comment.getContent())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .parentCommentId(comment.getParentComment() != null
                        ? comment.getParentComment().getCommentId()
                        : null)
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .replyCount(replyCount)
                .build();
    }
}
