package com.capstone.be.domain.page_comment.controller;

import com.capstone.be.domain.page_comment.dto.request.PageCommentCreateRequest;
import com.capstone.be.domain.page_comment.dto.request.PageCommentReplyCreateRequest;
import com.capstone.be.domain.page_comment.dto.request.PageCommentUpdateRequest;
import com.capstone.be.domain.page_comment.dto.response.PageCommentListResponse;
import com.capstone.be.domain.page_comment.dto.response.PageCommentResponse;
import com.capstone.be.domain.page_comment.enums.CommentSortType;
import com.capstone.be.domain.page_comment.service.PageCommentService;
import com.capstone.be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/page-comments")
@RequiredArgsConstructor
@Validated
@Tag(name = "PageComment", description = "페이지 댓글, 좋아요, 대댓글 관련 api")
public class PageCommentController {

    private final PageCommentService pageCommentService;

    // 댓글 생성
    @PostMapping("/{pageId}")
    @Operation(summary = "댓글 달기",
            description = "특정 문장(sentenceId)에 댓글을 작성합니다. sentenceId를 생략(null)하면 "
                    + "특정 문장이 아니라 페이지 전체에 다는 댓글로 저장됩니다.")
    public ResponseEntity<BaseResponse<PageCommentResponse>> createComment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long pageId,
            @Valid @RequestBody PageCommentCreateRequest request) {
        PageCommentResponse response = pageCommentService.createComment(authorization, pageId, request);
        return ResponseEntity.ok(BaseResponse.success("댓글이 등록되었습니다.", response));
    }

    // 댓글 가져오기
    @GetMapping("/{pageId}")
    @Operation(summary = "댓글 가져오기",
            description = "해당 페이지의 최상위 댓글 목록을 조회합니다. 비로그인 사용자도 조회할 수 있습니다. "
                    + "각 댓글에는 그 댓글에 달린 답글 수(replyCount)도 같이 내려갑니다.\n"
                    + "sort=LATEST(기본값, 최신순)는 무한스크롤 방식입니다 - 첫 요청은 cursor를 생략하고, "
                    + "이후에는 이전 응답의 nextCursor를 그대로 cursor로 보내면 이어서 다음 목록을 받습니다"
                    + "(이 경우 page는 무시됨).\n"
                    + "sort=POPULAR(인기순=좋아요 개수순)는 기존 방식대로 page/size로 페이징합니다.")
    public ResponseEntity<BaseResponse<PageCommentListResponse>> getComments(
            @PathVariable Long pageId,
            @RequestParam(defaultValue = "LATEST") CommentSortType sort,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        PageCommentListResponse response = pageCommentService.getComments(pageId, sort, cursor, page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 문장 단위 내 댓글 가져오기 (수집한 문장 상세보기용)
    @GetMapping("/sentence/{sentenceId}")
    @Operation(summary = "문장에 내가 남긴 댓글 가져오기",
            description = "특정 문장(sentenceId)에 로그인한 본인이 남긴 최상위 댓글 목록을 최신순 무한스크롤로 조회합니다. "
                    + "'수집한 문장 상세보기'에서 내가 그 문장에 남긴 댓글만 보고 싶을 때 사용하세요. "
                    + "첫 요청은 cursor를 생략하고, 이후에는 이전 응답의 nextCursor를 그대로 넘기면 됩니다.")
    public ResponseEntity<BaseResponse<PageCommentListResponse>> getCommentsBySentence(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long sentenceId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        PageCommentListResponse response =
                pageCommentService.getCommentsBySentence(authorization, sentenceId, cursor, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 댓글 수정
    @PatchMapping("/{pageId}/{commentId}")
    @Operation(summary = "댓글 수정하기", description = "본인이 작성한 댓글만 수정할 수 있습니다.")
    public ResponseEntity<BaseResponse<PageCommentResponse>> updateComment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long pageId,
            @PathVariable Long commentId,
            @Valid @RequestBody PageCommentUpdateRequest request) {
        PageCommentResponse response = pageCommentService.updateComment(authorization, pageId, commentId, request);
        return ResponseEntity.ok(BaseResponse.success("댓글이 수정되었습니다.", response));
    }

    // 댓글 삭제
    @DeleteMapping("/{pageId}/{commentId}")
    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글만 삭제할 수 있습니다.")
    public ResponseEntity<BaseResponse<Void>> deleteComment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long pageId,
            @PathVariable Long commentId) {
        pageCommentService.deleteComment(authorization, pageId, commentId);
        return ResponseEntity.ok(BaseResponse.success("댓글이 삭제되었습니다.", null));
    }



    /*
    * 대댓글 관련 api
    * */

    // 답글 작성
    @PostMapping("/{pageId}/{commentId}")
    @Operation(summary = "댓글의 답글 달기", description = "댓글(commentId)에 답글을 작성합니다. 답글에는 답글을 달 수 없습니다.")
    public ResponseEntity<BaseResponse<PageCommentResponse>> createReply(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long pageId,
            @PathVariable Long commentId,
            @Valid @RequestBody PageCommentReplyCreateRequest request) {
        PageCommentResponse response = pageCommentService.createReply(authorization, pageId, commentId, request);
        return ResponseEntity.ok(BaseResponse.success("답글이 등록되었습니다.", response));
    }

    // 답글 가져오기
    @GetMapping("/{pageId}/{commentId}")
    @Operation(summary = "댓글의 답글 가져오기",
            description = "댓글(commentId)에 달린 답글 목록을 작성 순서대로 무한스크롤 방식으로 조회합니다. "
                    + "첫 요청은 cursor를 생략하고, 이후에는 이전 응답의 nextCursor를 그대로 cursor로 보내면 "
                    + "이어서 다음 목록을 받습니다. 각 답글에는 작성자 프로필 이미지 URL"
                    + "(profileImageUrl, 없으면 null)도 같이 내려갑니다. 비로그인 사용자도 조회할 수 있습니다.")
    public ResponseEntity<BaseResponse<PageCommentListResponse>> getReplies(
            @PathVariable Long pageId,
            @PathVariable Long commentId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        PageCommentListResponse response = pageCommentService.getReplies(pageId, commentId, cursor, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 답글 수정
    @PatchMapping("/{pageId}/{commentId}/{replyId}")
    @Operation(summary = "댓글의 답글 수정하기", description = "본인이 작성한 답글만 수정할 수 있습니다.")
    public ResponseEntity<BaseResponse<PageCommentResponse>> updateReply(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long pageId,
            @PathVariable Long commentId,
            @PathVariable Long replyId,
            @Valid @RequestBody PageCommentUpdateRequest request) {
        PageCommentResponse response =
                pageCommentService.updateReply(authorization, pageId, commentId, replyId, request);
        return ResponseEntity.ok(BaseResponse.success("답글이 수정되었습니다.", response));
    }

    // 답글 삭제
    @DeleteMapping("/{pageId}/{commentId}/{replyId}")
    @Operation(summary = "댓글의 답글 지우기", description = "본인이 작성한 답글만 삭제할 수 있습니다.")
    public ResponseEntity<BaseResponse<Void>> deleteReply(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long pageId,
            @PathVariable Long commentId,
            @PathVariable Long replyId) {
        pageCommentService.deleteReply(authorization, pageId, commentId, replyId);
        return ResponseEntity.ok(BaseResponse.success("답글이 삭제되었습니다.", null));
    }

    /*
    * LIKE 관련 api
    * */

    // 좋아요 누르기
    @PostMapping("/{pageId}/{commentId}/like")
    @Operation(summary = "좋아요 누르기",
            description = "댓글/답글(commentId)에 좋아요를 남깁니다. 이미 싫어요를 누른 상태였다면 좋아요로 바뀝니다.")
    public ResponseEntity<BaseResponse<Void>> likeComment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long pageId,
            @PathVariable Long commentId) {
        pageCommentService.likeComment(authorization, pageId, commentId);
        return ResponseEntity.ok(BaseResponse.success("좋아요를 눌렀습니다.", null));
    }

    // 좋아요 취소하기
    @DeleteMapping("/{pageId}/{commentId}/like")
    @Operation(summary = "좋아요 취소하기",
            description = "좋아요를 취소합니다. 좋아요 상태가 아니었다면 아무 일도 일어나지 않습니다.")
    public ResponseEntity<BaseResponse<Void>> cancelLike(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long pageId,
            @PathVariable Long commentId) {
        pageCommentService.cancelLike(authorization, pageId, commentId);
        return ResponseEntity.ok(BaseResponse.success("좋아요를 취소했습니다.", null));
    }

    /*
     * DISLIKE 관련 api
     * */

    // 싫어요 누르기
    @PostMapping("/{pageId}/{commentId}/dislike")
    @Operation(summary = "싫어요 누르기",
            description = "댓글/답글(commentId)에 싫어요를 남깁니다. 이미 좋아요를 누른 상태였다면 싫어요로 바뀝니다.")
    public ResponseEntity<BaseResponse<Void>> dislikeComment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long pageId,
            @PathVariable Long commentId) {
        pageCommentService.dislikeComment(authorization, pageId, commentId);
        return ResponseEntity.ok(BaseResponse.success("싫어요를 눌렀습니다.", null));
    }

    // 싫어요 취소하기
    @DeleteMapping("/{pageId}/{commentId}/dislike")
    @Operation(summary = "싫어요 취소하기",
            description = "싫어요를 취소합니다. 싫어요 상태가 아니었다면 아무 일도 일어나지 않습니다.")
    public ResponseEntity<BaseResponse<Void>> cancelDislike(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long pageId,
            @PathVariable Long commentId) {
        pageCommentService.cancelDislike(authorization, pageId, commentId);
        return ResponseEntity.ok(BaseResponse.success("싫어요를 취소했습니다.", null));
    }
}
