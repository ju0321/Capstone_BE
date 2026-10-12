package com.capstone.be.domain.book_sentences.controller;

import com.capstone.be.domain.book_sentences.dto.request.SentenceHighlightColorUpdateRequest;
import com.capstone.be.domain.book_sentences.dto.request.SentenceHighlightCreateRequest;
import com.capstone.be.domain.book_sentences.dto.request.SentenceHighlightMemoUpdateRequest;
import com.capstone.be.domain.book_sentences.dto.response.SentenceHighlightListItemResponse;
import com.capstone.be.domain.book_sentences.dto.response.SentenceHighlightResponse;
import com.capstone.be.domain.book_sentences.service.SentenceService;
import com.capstone.be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/highlights")
@RequiredArgsConstructor
@Tag(name = "Sentence", description = "문장 관련 API")
public class SentenceController {

    private final SentenceService sentenceService;

    @PostMapping
    @Operation(summary = "문장 수집하기",
            description = "로그인한 유저가 드래그로 선택한 문장(sentenceId)에 형광펜 색상(color)을 지정해서 수집합니다. "
                    + "이미 그 문장을 수집한 적이 있으면 색상/내용만 갱신됩니다(유저당 문장 하나에 하이라이트 하나).")
    public ResponseEntity<BaseResponse<SentenceHighlightResponse>> collectHighlight(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody SentenceHighlightCreateRequest request) {
        SentenceHighlightResponse response = sentenceService.createHighlight(authorization, request);
        return ResponseEntity.ok(BaseResponse.success("문장을 수집했습니다.", response));
    }

    @GetMapping
    @Operation(summary = "수집한 문장 목록 조회하기",
            description = "로그인한 유저가 수집한 문장 목록을 최신순으로 조회합니다. "
                    + "bookId를 주면 해당 책에서 수집한 문장만 조회됩니다.")
    public ResponseEntity<BaseResponse<List<SentenceHighlightListItemResponse>>> getMyHighlights(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long bookId) {
        List<SentenceHighlightListItemResponse> response = sentenceService.getMyHighlights(authorization, bookId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @GetMapping("/{sentenceId}")
    @Operation(summary = "수집한 문장 상세보기",
            description = "로그인한 유저가 수집한 문장(sentenceId) 하나의 상세 정보를 조회합니다. "
                    + "책/페이지 정보와 메모를 함께 반환합니다.")
    public ResponseEntity<BaseResponse<SentenceHighlightListItemResponse>> getHighlightDetail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long sentenceId) {
        SentenceHighlightListItemResponse response = sentenceService.getHighlightDetail(authorization, sentenceId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PatchMapping("/{sentenceId}/memo")
    @Operation(summary = "메모 작성/수정하기",
            description = "로그인한 유저가 수집한 문장(sentenceId)에 메모를 남기거나 수정합니다. "
                    + "memo를 비워서 보내면 메모가 삭제됩니다.")
    public ResponseEntity<BaseResponse<SentenceHighlightListItemResponse>> updateHighlightMemo(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long sentenceId,
            @Valid @RequestBody SentenceHighlightMemoUpdateRequest request) {
        SentenceHighlightListItemResponse response =
                sentenceService.updateHighlightMemo(authorization, sentenceId, request);
        return ResponseEntity.ok(BaseResponse.success("메모를 저장했습니다.", response));
    }

    @PatchMapping("/{sentenceId}")
    @Operation(summary = "하이라이트 색상 변경하기",
            description = "로그인한 유저가 이미 수집한 문장(sentenceId)의 형광펜 색상을 변경합니다.")
    public ResponseEntity<BaseResponse<SentenceHighlightResponse>> updateHighlightColor(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long sentenceId,
            @Valid @RequestBody SentenceHighlightColorUpdateRequest request) {
        SentenceHighlightResponse response = sentenceService.updateHighlightColor(authorization, sentenceId, request);
        return ResponseEntity.ok(BaseResponse.success("하이라이트 색상을 변경했습니다.", response));
    }

    @DeleteMapping("/{sentenceId}")
    @Operation(summary = "하이라이트 삭제하기",
            description = "로그인한 유저가 수집한 문장(sentenceId)의 하이라이트를 삭제합니다.")
    public ResponseEntity<BaseResponse<Void>> deleteHighlight(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long sentenceId) {
        sentenceService.deleteHighlight(authorization, sentenceId);
        return ResponseEntity.ok(BaseResponse.success("하이라이트를 삭제했습니다.", null));
    }
}
