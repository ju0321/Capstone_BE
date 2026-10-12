package com.capstone.be.domain.page_comment.repository;

import com.capstone.be.domain.page_comment.entity.PageComment;
import com.capstone.be.domain.page_comment.enums.ReactionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PageCommentRepository extends JpaRepository<PageComment, Long> {

    /**
     * 이 페이지에 달린 "댓글 수" - 최상위 댓글(parentComment가 없는 것)만 센다. 답글은
     * 포함하지 않는다. "책 페이지 내용 가져오기"(CuttonPageService.getPageContentForUser())에서
     * 이 페이지의 댓글 수를 같이 내려줄 때 쓴다.
     */
    long countByPage_PageIdAndParentCommentIsNull(Long pageId);

    /**
     * 여러 페이지의 "댓글 수"(최상위 댓글만, 답글 제외)를 한 번에 조회한다. "진행 중인 챕터
     * 내용 가져오기"/"책 전체 내용 가져오기"에서 페이지마다 댓글 수를 내려줄 때 N+1을 피하기
     * 위해 쓴다(PageRepository.findMaxPageNumberByBookIds()와 동일한 배치 조회 패턴).
     * 댓글이 하나도 없는 페이지는 결과에 아예 나타나지 않으므로, 호출하는 쪽에서 0으로
     * 기본값 처리해야 한다.
     */
    @Query("SELECT c.page.pageId AS pageId, COUNT(c) AS commentCount FROM PageComment c "
            + "WHERE c.page.pageId IN :pageIds AND c.parentComment IS NULL GROUP BY c.page.pageId")
    List<PageCommentCountProjection> countRootCommentsByPageIds(@Param("pageIds") List<Long> pageIds);

    /**
     * 특정 댓글에 달린 "답글 수". 답글에는 답글을 달 수 없으므로(2단계 중첩 금지), 답글
     * 자신의 commentId로 호출하면 항상 0이다. 댓글 단건 응답(수정 등)에서 답글 수를 같이
     * 내려줄 때 쓴다.
     */
    long countByParentComment_CommentId(Long parentCommentId);

    /**
     * 여러 댓글의 "답글 수"를 한 번에 조회한다. 댓글 목록 조회(getComments 등)에서 항목마다
     * 답글 수를 내려줄 때 N+1을 피하기 위해 쓴다(countRootCommentsByPageIds()와 동일한 배치
     * 조회 패턴). 답글이 하나도 없는 댓글은 결과에 아예 나타나지 않으므로, 호출하는 쪽에서
     * 0으로 기본값 처리해야 한다.
     */
    @Query("SELECT c.parentComment.commentId AS parentCommentId, COUNT(c) AS replyCount FROM PageComment c "
            + "WHERE c.parentComment.commentId IN :commentIds GROUP BY c.parentComment.commentId")
    List<PageCommentReplyCountProjection> countRepliesByParentCommentIds(@Param("commentIds") List<Long> commentIds);

    /**
     * 최상위 댓글 최신순 무한스크롤. cursor는 바로 이전 응답의 nextCursor(=마지막 댓글의
     * commentId) - 첫 요청이면 null, 그 뒤로는 이 값보다 작은(더 먼저 작성된) 댓글부터 이어서
     * 내려준다. commentId는 auto-increment라 작성 순서와 동일해서 정렬/커서 기준으로 같이 쓴다.
     * pageable은 hasNext 판단을 위해 size+1개를 요청하는 용도로만 쓴다(offset은 항상 0).
     */
    @Query("""
            SELECT c FROM PageComment c
            WHERE c.page.pageId = :pageId AND c.parentComment IS NULL
              AND (:cursor IS NULL OR c.commentId < :cursor)
            ORDER BY c.commentId DESC
            """)
    List<PageComment> findLatestTopLevelWithCursor(
            @Param("pageId") Long pageId, @Param("cursor") Long cursor, Pageable pageable);

    // 인기순: 좋아요(type=LIKE) 개수 내림차순, 동점이면 최신순으로 2차 정렬한다. (offset 페이징 유지)
    @Query(value = """
            SELECT c FROM PageComment c
            LEFT JOIN CommentReaction r ON r.comment = c AND r.type = :likeType
            WHERE c.page.pageId = :pageId AND c.parentComment IS NULL
            GROUP BY c
            ORDER BY COUNT(r) DESC, c.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(c) FROM PageComment c
            WHERE c.page.pageId = :pageId AND c.parentComment IS NULL
            """)
    Page<PageComment> findByPageOrderByLikeCountDesc(
            @Param("pageId") Long pageId, @Param("likeType") ReactionType likeType, Pageable pageable);

    /**
     * 특정 문장(sentenceId)에 본인(userId)이 남긴 최상위 댓글 최신순 무한스크롤.
     * ("수집한 문장 상세보기"에서 내가 그 문장에 남긴 댓글만 보여줄 때 사용).
     */
    @Query("""
            SELECT c FROM PageComment c
            WHERE c.sentence.sentenceId = :sentenceId AND c.user.userId = :userId
              AND c.parentComment IS NULL
              AND (:cursor IS NULL OR c.commentId < :cursor)
            ORDER BY c.commentId DESC
            """)
    List<PageComment> findLatestBySentenceAndUserWithCursor(
            @Param("sentenceId") Long sentenceId, @Param("userId") Long userId,
            @Param("cursor") Long cursor, Pageable pageable);

    /**
     * 특정 댓글에 달린 답글 목록 무한스크롤(작성 순서 = commentId 오름차순). cursor는 바로 이전
     * 응답의 nextCursor(=마지막 답글의 commentId) - 첫 요청이면 null, 그 뒤로는 이 값보다 큰
     * (더 나중에 작성된) 답글부터 이어서 내려준다.
     */
    @Query("""
            SELECT c FROM PageComment c
            WHERE c.parentComment.commentId = :commentId
              AND (:cursor IS NULL OR c.commentId > :cursor)
            ORDER BY c.commentId ASC
            """)
    List<PageComment> findRepliesWithCursor(
            @Param("commentId") Long commentId, @Param("cursor") Long cursor, Pageable pageable);
}
