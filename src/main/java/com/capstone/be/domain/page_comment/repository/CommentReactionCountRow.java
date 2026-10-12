package com.capstone.be.domain.page_comment.repository;

import com.capstone.be.domain.page_comment.enums.ReactionType;

/**
 * 댓글별 좋아요/싫어요 개수 집계 결과를 위한 프로젝션.
 * 댓글 하나당 반응 타입(LIKE/DISLIKE)별로 한 행씩 나온다 (반응이 없으면 그 타입 행 자체가 없음).
 */
public interface CommentReactionCountRow {
    Long getCommentId();
    ReactionType getType();
    Long getCnt();
}
