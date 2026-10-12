package com.capstone.be.domain.cutton_page.enums;

/**
 * 페이지에 대한 유저 반응 종류. 댓글의 좋아요/싫어요(ReactionType)와 달리 서로 배타적이지
 * 않아서(북마크도 하고 좋아요도 동시에 가능) 유저 한 명이 같은 페이지에 대해 BOOKMARK/LIKE
 * 행을 동시에 가질 수 있다 - 그래서 book_page_status의 유니크 제약도 (page_id, user_id)가
 * 아니라 (page_id, user_id, type) 3컬럼이다.
 */
public enum PageReactionType {
    BOOKMARK,
    LIKE
}
