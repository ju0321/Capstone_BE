package com.capstone.be.domain.page_comment.repository;

import com.capstone.be.domain.page_comment.entity.PageComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * 위해 쓴다.
     * 댓글이 하나도 없는 페이지는 결과에 아예 나타나지 않으므로, 호출하는 쪽에서 0으로
     * 기본값 처리해야 한다.
     */
    @Query("SELECT c.page.pageId AS pageId, COUNT(c) AS commentCount FROM PageComment c "
            + "WHERE c.page.pageId IN :pageIds AND c.parentComment IS NULL GROUP BY c.page.pageId")
    List<PageCommentCountProjection> countRootCommentsByPageIds(@Param("pageIds") List<Long> pageIds);
}
