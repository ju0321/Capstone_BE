package com.capstone.be.domain.page_comment.repository;

/** PageCommentRepository.countRootCommentsByPageIds()의 조회 결과 프로젝션. */
public interface PageCommentCountProjection {
    Long getPageId();
    Long getCommentCount();
}
