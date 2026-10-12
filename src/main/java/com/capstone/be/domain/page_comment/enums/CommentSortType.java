package com.capstone.be.domain.page_comment.enums;

/**
 * 댓글 목록 정렬 기준.
 * LATEST: 최신순 (createdAt 내림차순)
 * POPULAR: 인기순 (좋아요 개수 내림차순, 동점이면 최신순)
 *
 * TODO: "찐팬 댓글"(작성자의 활동량/충성도 등을 기준으로 하는 정렬) 옵션이 추후 추가될 예정.
 *  판단 기준 자체가 아직 정해지지 않아서 지금은 값을 만들지 않고 자리만 남겨둔다.
 */
public enum CommentSortType {
    LATEST,
    POPULAR
}
