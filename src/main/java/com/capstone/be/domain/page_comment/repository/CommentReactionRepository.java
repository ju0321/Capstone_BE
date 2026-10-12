package com.capstone.be.domain.page_comment.repository;

import com.capstone.be.domain.page_comment.entity.CommentReaction;
import com.capstone.be.domain.page_comment.enums.ReactionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    // 현재 이 유저가 이 댓글에 남긴 반응(있다면 하나뿐)을 조회한다.
    // 좋아요<->싫어요 반대 반응을 눌렀을 때 "이미 좋아요/싫어요 누른 댓글입니다"를 판단하는 데 사용한다.
    Optional<CommentReaction> findByComment_CommentIdAndUser_UserId(Long commentId, Long userId);

    // 좋아요/싫어요를 원자적으로 기록한다. (comment_id, user_id)가 유니크라서,
    // 이미 반응이 있으면 type만 갈아끼우고(좋아요<->싫어요 전환), 없으면 새로 만든다.
    // book_page_status 때와 같은 이유로, 엔티티를 읽어와서 값 바꾸고 저장하는 방식(non-atomic)이 아니라
    // INSERT ... ON CONFLICT DO UPDATE로 한 번에 처리해서 동시 요청/트랜잭션 중단 문제를 피한다.
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO comment_reactions (comment_id, user_id, type, created_at)
            VALUES (:commentId, :userId, :type, now())
            ON CONFLICT (comment_id, user_id) DO UPDATE SET type = EXCLUDED.type
            """, nativeQuery = true)
    void upsertReaction(@Param("commentId") Long commentId, @Param("userId") Long userId, @Param("type") String type);

    // 현재 반응이 정확히 그 타입일 때만 지운다. (예: 싫어요 상태에서 "좋아요 취소" 요청이 오면 아무 일도
    // 안 일어나야 하고, 애초에 반응이 없어도 그냥 조용히 0건 삭제로 끝나야 함 - 멱등한 취소)
    @Modifying(clearAutomatically = true)
    void deleteByComment_CommentIdAndUser_UserIdAndType(Long commentId, Long userId, ReactionType type);

    // 주어진 댓글 id들에 대해, 댓글별/반응타입별 개수를 한 번의 쿼리로 집계한다.
    // (목록 조회에서 댓글마다 따로 카운트 쿼리를 날리는 N+1을 피하기 위함)
    @Query("""
            SELECT r.comment.commentId AS commentId, r.type AS type, COUNT(r) AS cnt
            FROM CommentReaction r
            WHERE r.comment.commentId IN :commentIds
            GROUP BY r.comment.commentId, r.type
            """)
    List<CommentReactionCountRow> countByCommentIds(@Param("commentIds") List<Long> commentIds);
}
