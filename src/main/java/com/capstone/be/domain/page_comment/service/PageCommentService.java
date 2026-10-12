package com.capstone.be.domain.page_comment.service;

import com.capstone.be.domain.book.entity.Page;
import com.capstone.be.domain.book.entity.Sentence;
import com.capstone.be.domain.book.repository.PageRepository;
import com.capstone.be.domain.book.repository.SentenceRepository;
import com.capstone.be.domain.cutton_page.error.CuttonPageErrorCode;
import com.capstone.be.domain.page_comment.dto.request.PageCommentCreateRequest;
import com.capstone.be.domain.page_comment.dto.request.PageCommentReplyCreateRequest;
import com.capstone.be.domain.page_comment.dto.request.PageCommentUpdateRequest;
import com.capstone.be.domain.page_comment.dto.response.PageCommentResponse;
import com.capstone.be.domain.page_comment.dto.response.PageCommentListResponse;
import com.capstone.be.domain.page_comment.entity.PageComment;
import com.capstone.be.domain.page_comment.enums.CommentSortType;
import com.capstone.be.domain.page_comment.enums.PageCommentStatus;
import com.capstone.be.domain.page_comment.enums.ReactionType;
import com.capstone.be.domain.page_comment.error.PageCommentErrorCode;
import com.capstone.be.domain.page_comment.repository.CommentReactionCountRow;
import com.capstone.be.domain.page_comment.repository.CommentReactionRepository;
import com.capstone.be.domain.page_comment.repository.PageCommentRepository;
import com.capstone.be.domain.page_comment.repository.PageCommentReplyCountProjection;
import com.capstone.be.domain.user.entity.User;
import com.capstone.be.domain.user.repository.UserRepository;
import com.capstone.be.global.exception.CustomException;
import com.capstone.be.global.exception.GlobalErrorCode;
import com.capstone.be.global.util.JwtUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PageCommentService {

    private final PageCommentRepository pageCommentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final PageRepository pageRepository;
    private final SentenceRepository sentenceRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * 댓글을 작성합니다. sentenceId가 있으면 실제로 해당 pageId에 속하는 문장인지 검증합니다.
     * sentenceId가 없으면(null) 특정 문장이 아니라 페이지 전체에 다는 댓글로 저장합니다.
     */
    @Transactional
    public PageCommentResponse createComment(String authorizationHeader, Long pageId,
                                              PageCommentCreateRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new CustomException(CuttonPageErrorCode.PAGE_NOT_FOUND));

        Sentence sentence = null;
        if (request.getSentenceId() != null) {
            sentence = sentenceRepository.findById(request.getSentenceId())
                    .orElseThrow(() -> new CustomException(PageCommentErrorCode.SENTENCE_NOT_FOUND));
            if (!sentence.getPage().getPageId().equals(pageId)) {
                throw new CustomException(PageCommentErrorCode.SENTENCE_NOT_FOUND);
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        // status(스포일러 여부)는 클라이언트가 판단할 수 없는 값이라 항상 COMMON으로 저장한다.
        // TODO: 추후 AI가 content를 읽고 스포일러 여부를 판단해서 상태를 갱신하는 로직 추가 예정.
        PageComment comment = PageComment.builder()
                .user(user)
                .page(page)
                .sentence(sentence)
                .content(request.getContent())
                .status(PageCommentStatus.COMMON)
                .build();

        PageComment saved = pageCommentRepository.save(comment);
        return PageCommentResponse.from(saved);
    }

    /**
     * 페이지의 최상위 댓글 목록을 조회합니다. (답글은 아직 이 API에서 다루지 않습니다.)
     * 비로그인 사용자도 조회할 수 있습니다.
     * <p>
     * sort=LATEST(기본값)는 무한스크롤(커서 기반)입니다 - cursor/size만 쓰고 page는 무시됩니다.
     * sort=POPULAR(좋아요순)는 좋아요 개수가 계속 바뀌어서 커서로 안정적으로 이어붙이기 어렵기
     * 때문에 기존 방식대로 offset 페이징(page/size)을 그대로 씁니다.
     */
    @Transactional(readOnly = true)
    public PageCommentListResponse getComments(Long pageId, CommentSortType sort, Long cursor, int page, int size) {
        if (!pageRepository.existsById(pageId)) {
            throw new CustomException(CuttonPageErrorCode.PAGE_NOT_FOUND);
        }

        return switch (sort) {
            case LATEST -> getLatestCommentsByCursor(pageId, cursor, size);
            case POPULAR -> getPopularCommentsByPage(pageId, page, size);
        };
    }

    private PageCommentListResponse getLatestCommentsByCursor(Long pageId, Long cursor, int size) {
        // hasNext를 별도 COUNT 쿼리 없이 판단하기 위해 size보다 1개 더 가져온다.
        List<PageComment> fetched = pageCommentRepository.findLatestTopLevelWithCursor(
                pageId, cursor, PageRequest.of(0, size + 1));
        boolean hasNext = fetched.size() > size;
        List<PageComment> content = hasNext ? fetched.subList(0, size) : fetched;

        Long nextCursor = hasNext ? content.get(content.size() - 1).getCommentId() : null;

        return PageCommentListResponse.builder()
                .comments(toResponses(content))
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    /**
     * 특정 문장(sentenceId)에 로그인한 유저 본인이 남긴 최상위 댓글 목록을 최신순 무한스크롤로 조회합니다.
     * "수집한 문장 상세보기"에서 내가 그 문장에 남긴 댓글만 보고 싶을 때 사용합니다.
     */
    @Transactional(readOnly = true)
    public PageCommentListResponse getCommentsBySentence(String authorizationHeader, Long sentenceId, Long cursor,
                                                           int size) {
        Long userId = resolveUserId(authorizationHeader);

        if (!sentenceRepository.existsById(sentenceId)) {
            throw new CustomException(PageCommentErrorCode.SENTENCE_NOT_FOUND);
        }

        List<PageComment> fetched = pageCommentRepository.findLatestBySentenceAndUserWithCursor(
                sentenceId, userId, cursor, PageRequest.of(0, size + 1));
        boolean hasNext = fetched.size() > size;
        List<PageComment> content = hasNext ? fetched.subList(0, size) : fetched;

        Long nextCursor = hasNext ? content.get(content.size() - 1).getCommentId() : null;

        return PageCommentListResponse.builder()
                .comments(toResponses(content))
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    private PageCommentListResponse getPopularCommentsByPage(Long pageId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        org.springframework.data.domain.Page<PageComment> result =
                pageCommentRepository.findByPageOrderByLikeCountDesc(pageId, ReactionType.LIKE, pageable);

        return PageCommentListResponse.builder()
                .comments(toResponses(result.getContent()))
                .page(page)
                .size(size)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .hasNext(result.hasNext())
                .build();
    }

    /**
     * 댓글을 수정합니다. 본인이 작성한 댓글만 수정할 수 있습니다.
     */
    @Transactional
    public PageCommentResponse updateComment(String authorizationHeader, Long pageId, Long commentId,
                                              PageCommentUpdateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        PageComment comment = getOwnedComment(pageId, commentId, userId);

        // status(스포일러 여부)는 이 요청으로 바뀌지 않는다 - 기존 값 그대로 유지.
        comment.update(request.getContent());

        return toResponseWithCounts(comment, loadReactionCounts(List.of(comment.getCommentId())),
                loadReplyCounts(List.of(comment.getCommentId())));
    }

    /**
     * 댓글을 삭제합니다. 본인이 작성한 댓글만 삭제할 수 있습니다.
     * (해당 댓글에 달린 답글을 어떻게 처리할지는 답글 기능을 구현할 때 함께 정할 예정입니다.)
     */
    @Transactional
    public void deleteComment(String authorizationHeader, Long pageId, Long commentId) {
        Long userId = resolveUserId(authorizationHeader);
        PageComment comment = getOwnedComment(pageId, commentId, userId);
        pageCommentRepository.delete(comment);
    }

    /**
     * 댓글에 답글을 작성합니다. 원댓글(commentId)이 이미 답글이면(2단계 중첩) 거부합니다.
     * sentenceId/status는 원댓글 것을 그대로 물려받습니다.
     */
    @Transactional
    public PageCommentResponse createReply(String authorizationHeader, Long pageId, Long commentId,
                                            PageCommentReplyCreateRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        PageComment parent = pageCommentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(PageCommentErrorCode.COMMENT_NOT_FOUND));
        if (!parent.getPage().getPageId().equals(pageId)) {
            throw new CustomException(PageCommentErrorCode.COMMENT_NOT_FOUND);
        }
        if (parent.getParentComment() != null) {
            throw new CustomException(PageCommentErrorCode.REPLY_TO_REPLY_NOT_ALLOWED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        PageComment reply = PageComment.builder()
                .user(user)
                .page(parent.getPage())
                .sentence(parent.getSentence())
                .content(request.getContent())
                .status(PageCommentStatus.COMMON)
                .parentComment(parent)
                .build();

        PageComment saved = pageCommentRepository.save(reply);
        return PageCommentResponse.from(saved);
    }

    /**
     * 댓글에 달린 답글 목록을 작성 순서대로 무한스크롤(커서 기반)로 조회합니다.
     * 비로그인 사용자도 조회할 수 있습니다.
     */
    @Transactional(readOnly = true)
    public PageCommentListResponse getReplies(Long pageId, Long commentId, Long cursor, int size) {
        PageComment parent = pageCommentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(PageCommentErrorCode.COMMENT_NOT_FOUND));
        if (!parent.getPage().getPageId().equals(pageId)) {
            throw new CustomException(PageCommentErrorCode.COMMENT_NOT_FOUND);
        }

        // hasNext를 별도 COUNT 쿼리 없이 판단하기 위해 size보다 1개 더 가져온다.
        List<PageComment> fetched = pageCommentRepository.findRepliesWithCursor(
                commentId, cursor, PageRequest.of(0, size + 1));
        boolean hasNext = fetched.size() > size;
        List<PageComment> content = hasNext ? fetched.subList(0, size) : fetched;

        Long nextCursor = hasNext ? content.get(content.size() - 1).getCommentId() : null;

        return PageCommentListResponse.builder()
                .comments(toResponses(content))
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    /**
     * 답글을 수정합니다. 본인이 작성한 답글만 수정할 수 있습니다.
     */
    @Transactional
    public PageCommentResponse updateReply(String authorizationHeader, Long pageId, Long commentId, Long replyId,
                                            PageCommentUpdateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        PageComment reply = getOwnedReply(pageId, commentId, replyId, userId);
        reply.update(request.getContent());
        return toResponseWithCounts(reply, loadReactionCounts(List.of(reply.getCommentId())),
                loadReplyCounts(List.of(reply.getCommentId())));
    }

    /**
     * 답글을 삭제합니다. 본인이 작성한 답글만 삭제할 수 있습니다.
     */
    @Transactional
    public void deleteReply(String authorizationHeader, Long pageId, Long commentId, Long replyId) {
        Long userId = resolveUserId(authorizationHeader);
        PageComment reply = getOwnedReply(pageId, commentId, replyId, userId);
        pageCommentRepository.delete(reply);
    }

    /**
     * 댓글/답글에 좋아요를 남깁니다. 이미 싫어요를 눌렀던 상태라면 좋아요로 전환됩니다.
     */
    @Transactional
    public void likeComment(String authorizationHeader, Long pageId, Long commentId) {
        reactToComment(authorizationHeader, pageId, commentId, ReactionType.LIKE);
    }

    /**
     * 댓글/답글에 싫어요를 남깁니다. 이미 좋아요를 눌렀던 상태라면 싫어요로 전환됩니다.
     */
    @Transactional
    public void dislikeComment(String authorizationHeader, Long pageId, Long commentId) {
        reactToComment(authorizationHeader, pageId, commentId, ReactionType.DISLIKE);
    }

    /**
     * 좋아요를 취소합니다. 현재 좋아요 상태가 아니었다면(반응이 없거나 싫어요 상태) 아무 일도 하지 않습니다.
     */
    @Transactional
    public void cancelLike(String authorizationHeader, Long pageId, Long commentId) {
        cancelReaction(authorizationHeader, pageId, commentId, ReactionType.LIKE);
    }

    /**
     * 싫어요를 취소합니다. 현재 싫어요 상태가 아니었다면(반응이 없거나 좋아요 상태) 아무 일도 하지 않습니다.
     */
    @Transactional
    public void cancelDislike(String authorizationHeader, Long pageId, Long commentId) {
        cancelReaction(authorizationHeader, pageId, commentId, ReactionType.DISLIKE);
    }

    private void reactToComment(String authorizationHeader, Long pageId, Long commentId, ReactionType type) {
        Long userId = resolveUserId(authorizationHeader);
        validateCommentBelongsToPage(pageId, commentId);
        if (!userRepository.existsById(userId)) {
            throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
        }

        // 좋아요/싫어요는 둘 중 하나만 가능 - 반대 반응이 이미 있으면 조용히 덮어쓰지 않고 명시적으로 거부한다.
        // (같은 반응을 또 누르는 건 그대로 두어도 무해하니 통과시킨다 - 이 경우엔 취소는 DELETE로 별도 처리)
        commentReactionRepository.findByComment_CommentIdAndUser_UserId(commentId, userId)
                .ifPresent(existing -> {
                    if (existing.getType() != type) {
                        throw new CustomException(
                                type == ReactionType.LIKE
                                        ? PageCommentErrorCode.ALREADY_DISLIKED
                                        : PageCommentErrorCode.ALREADY_LIKED);
                    }
                });

        commentReactionRepository.upsertReaction(commentId, userId, type.name());
    }

    private void cancelReaction(String authorizationHeader, Long pageId, Long commentId, ReactionType type) {
        Long userId = resolveUserId(authorizationHeader);
        validateCommentBelongsToPage(pageId, commentId);
        commentReactionRepository.deleteByComment_CommentIdAndUser_UserIdAndType(commentId, userId, type);
    }

    private void validateCommentBelongsToPage(Long pageId, Long commentId) {
        PageComment comment = pageCommentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(PageCommentErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getPage().getPageId().equals(pageId)) {
            throw new CustomException(PageCommentErrorCode.COMMENT_NOT_FOUND);
        }
    }

    // getOwnedComment()로 존재/페이지 소속/작성자 본인 여부를 먼저 확인한 뒤,
    // 그 댓글이 실제로 commentId의 답글이 맞는지(부모가 일치하는지)까지 추가로 검증한다.
    private PageComment getOwnedReply(Long pageId, Long commentId, Long replyId, Long userId) {
        PageComment reply = getOwnedComment(pageId, replyId, userId);
        if (reply.getParentComment() == null || !reply.getParentComment().getCommentId().equals(commentId)) {
            throw new CustomException(PageCommentErrorCode.COMMENT_NOT_FOUND);
        }
        return reply;
    }

    private PageComment getOwnedComment(Long pageId, Long commentId, Long userId) {
        PageComment comment = pageCommentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(PageCommentErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getPage().getPageId().equals(pageId)) {
            throw new CustomException(PageCommentErrorCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getUser().getUserId().equals(userId)) {
            throw new CustomException(PageCommentErrorCode.COMMENT_ACCESS_DENIED);
        }
        return comment;
    }

    // 댓글/답글 엔티티 목록을 N+1 없이 응답 DTO 목록으로 변환한다
    // (작성자 워밍 + 반응 카운트/답글 수 배치 조회). 답글 목록(getReplies)에도 그대로 쓰이는데,
    // 답글에는 답글을 달 수 없어서 이 경우 replyCount는 항상 0으로 나온다.
    private List<PageCommentResponse> toResponses(List<PageComment> content) {
        warmUsers(content);
        List<Long> commentIds = content.stream().map(PageComment::getCommentId).toList();
        Map<Long, ReactionCounts> counts = loadReactionCounts(commentIds);
        Map<Long, Long> replyCounts = loadReplyCounts(commentIds);
        return content.stream()
                .map(c -> toResponseWithCounts(c, counts, replyCounts))
                .toList();
    }

    // 목록에 있는 댓글들의 작성자(User)를 한 번의 IN 쿼리로 미리 영속성 컨텍스트에 올려둔다.
    // PageComment.user는 LAZY라서 그냥 두면 PageCommentResponse.from()에서 comment.getUser().getNickname()을
    // 호출할 때마다 댓글 개수만큼 추가 쿼리(N+1)가 나간다. 여기서 미리 User를 로딩해두면, 이후
    // 같은 트랜잭션(영속성 컨텍스트) 안에서 지연 프록시가 초기화될 때 DB를 다시 안 타고 캐시를 그대로 쓴다.
    // (getUserId()는 FK 컬럼 값이라 프록시에서 바로 꺼낼 수 있어서 이 시점엔 쿼리가 안 나간다)
    private void warmUsers(List<PageComment> comments) {
        List<Long> userIds = comments.stream()
                .map(c -> c.getUser().getUserId())
                .distinct()
                .toList();
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds);
        }
    }

    // 댓글별 좋아요/싫어요 개수를 한 번의 쿼리로 모아서 Map으로 돌려준다.
    // (집계 행이 없는 타입은 0개라는 뜻이라, 읽을 때 getOrDefault로 0 처리한다)
    private Map<Long, ReactionCounts> loadReactionCounts(List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> likeCounts = new HashMap<>();
        Map<Long, Long> dislikeCounts = new HashMap<>();
        for (CommentReactionCountRow row : commentReactionRepository.countByCommentIds(commentIds)) {
            if (row.getType() == ReactionType.LIKE) {
                likeCounts.put(row.getCommentId(), row.getCnt());
            } else {
                dislikeCounts.put(row.getCommentId(), row.getCnt());
            }
        }

        Map<Long, ReactionCounts> result = new HashMap<>();
        for (Long commentId : commentIds) {
            result.put(commentId, new ReactionCounts(
                    likeCounts.getOrDefault(commentId, 0L),
                    dislikeCounts.getOrDefault(commentId, 0L)));
        }
        return result;
    }

    // 댓글별 답글 수를 한 번의 쿼리로 모아서 Map으로 돌려준다.
    // (답글이 없는 댓글은 결과에 없으므로, 읽을 때 getOrDefault로 0 처리한다)
    private Map<Long, Long> loadReplyCounts(List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Map.of();
        }
        return pageCommentRepository.countRepliesByParentCommentIds(commentIds).stream()
                .collect(Collectors.toMap(
                        PageCommentReplyCountProjection::getParentCommentId,
                        PageCommentReplyCountProjection::getReplyCount));
    }

    private PageCommentResponse toResponseWithCounts(PageComment comment, Map<Long, ReactionCounts> counts,
                                                       Map<Long, Long> replyCounts) {
        ReactionCounts reactionCounts = counts.getOrDefault(comment.getCommentId(), ReactionCounts.ZERO);
        long replyCount = replyCounts.getOrDefault(comment.getCommentId(), 0L);
        return PageCommentResponse.from(
                comment, reactionCounts.likeCount(), reactionCounts.dislikeCount(), replyCount);
    }

    private record ReactionCounts(long likeCount, long dislikeCount) {
        static final ReactionCounts ZERO = new ReactionCounts(0L, 0L);
    }

    private Long resolveUserId(String authorizationHeader) {
        return jwtUtil.getUserIdFromToken(extractAccessToken(authorizationHeader));
    }

    private String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }
        return authorizationHeader.substring("Bearer ".length());
    }
}
