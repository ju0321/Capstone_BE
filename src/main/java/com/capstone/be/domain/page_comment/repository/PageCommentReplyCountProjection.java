package com.capstone.be.domain.page_comment.repository;

/** PageCommentRepository.countRepliesByParentCommentIds()의 조회 결과 프로젝션. */
public interface PageCommentReplyCountProjection {
    Long getParentCommentId();
    Long getReplyCount();
}
